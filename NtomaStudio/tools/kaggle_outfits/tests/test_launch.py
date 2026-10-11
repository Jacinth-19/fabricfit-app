import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import launch
import outfit_job
from kaggle_client import VersionedKaggle


class WorkflowTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def prepare(self):
        return launch.prepare("testuser", self.root)

    def test_versioned_adapter_explicitly_sets_server_version_label(self):
        request = SimpleNamespace()
        VersionedKaggle.fill_request(request, "testuser/ntoma-outfits-test/4")
        self.assertEqual(request.user_name, "testuser")
        self.assertEqual(request.kernel_slug, "ntoma-outfits-test")
        self.assertEqual(request.version_label, "v4")
        with self.assertRaises(ValueError):
            VersionedKaggle.fill_request(request, "testuser/ntoma-outfits-test/latest")

    def test_offline_bundle_is_private_self_contained_and_has_no_environment(self):
        marker = "unrelated-private-environment-value"
        with patch.dict(os.environ, {"KAGGLE_API_TOKEN": marker}):
            state = self.prepare()
        upload = state.parent / "upload"
        self.assertEqual({p.name for p in upload.iterdir()}, {"kernel-metadata.json", "outfit-concepts.ipynb"})
        meta = json.loads((upload / "kernel-metadata.json").read_text())
        self.assertIs(meta["is_private"], True)
        self.assertIs(meta["enable_gpu"], True)
        self.assertEqual(meta["dataset_sources"], [])
        self.assertNotIn(marker, "".join(p.read_text() for p in upload.iterdir()))
        nb = json.loads((upload / "outfit-concepts.ipynb").read_text())
        self.assertEqual(nb["nbformat"], 4)
        for cell in nb["cells"]:
            if cell["cell_type"] == "code":
                compile("".join(cell["source"]), "notebook-cell", "exec")
                self.assertEqual(cell["outputs"], [])
        self.assertIn("use_safetensors=True", "".join(nb["cells"][-1]["source"]))
        self.assertNotIn("safety_checker=None", "".join(nb["cells"][-1]["source"]))

    def test_unique_jobs_and_invalid_username(self):
        self.assertNotEqual(self.prepare(), self.prepare())
        for username in ("../escape", "https://kaggle.com/name", "x", "user@email.test"):
            with self.assertRaises(launch.WorkflowError):
                launch.prepare(username, self.root)

    def test_prepare_only_does_not_import_kaggle(self):
        with patch.dict(sys.modules, {"kaggle": None}):
            self.assertEqual(launch.main(["--username", "testuser", "--work-dir", str(self.root)]), 0)

    def test_submission_requires_explicit_license_acknowledgement(self):
        with self.assertRaises(SystemExit) as exc:
            launch.main(["--username", "testuser", "--submit"])
        self.assertEqual(exc.exception.code, 2)

    def test_submit_persists_intent_and_tracks_returned_version(self):
        state = self.prepare()
        def push(*args, **kwargs):
            self.assertEqual(launch.read_state(state)["phase"], "submitting")
            self.assertEqual(kwargs, {"timeout": 3600, "acc": "NvidiaTeslaT4"})
            return SimpleNamespace(error="", version_number=3)
        api = Mock()
        api.kernels_push.side_effect = push
        result = launch.submit(api, state)
        self.assertEqual(launch.version_ref(result), result["kernel"] + "/3")
        self.assertEqual(result["phase"], "submitted")
        with self.assertRaises(launch.WorkflowError):
            launch.submit(api, state)
        api.kernels_push.assert_called_once()

    def test_uncertain_submission_does_not_repeat(self):
        state = self.prepare()
        api = Mock()
        api.kernels_push.side_effect = ConnectionError("lost connection")
        with self.assertRaises(ConnectionError):
            launch.submit(api, state)
        self.assertEqual(launch.read_state(state)["phase"], "submitting")
        with self.assertRaises(launch.WorkflowError):
            launch.submit(api, state)
        api.kernels_push.assert_called_once()

    def test_server_error_is_not_mistaken_for_success(self):
        state = self.prepare()
        api = Mock()
        api.kernels_push.return_value = SimpleNamespace(error="quota error", version_number=0)
        with self.assertRaises(launch.WorkflowError):
            launch.submit(api, state)
        self.assertEqual(launch.read_state(state)["phase"], "submitting")

    def test_public_metadata_is_rejected(self):
        state = self.prepare()
        path = state.parent / "upload/kernel-metadata.json"
        metadata = json.loads(path.read_text())
        metadata["is_private"] = False
        launch.write_json(path, metadata)
        api = Mock()
        with self.assertRaises(launch.WorkflowError):
            launch.submit(api, state)
        api.kernels_push.assert_not_called()

    def wait_with_statuses(self, statuses):
        api = Mock()
        api.kernels_status.side_effect = [SimpleNamespace(status=SimpleNamespace(name=s)) for s in statuses]
        now = [0.0]
        def sleep(n):
            now[0] += n
        result = launch.wait(api, "testuser/ntoma-outfits-test/1", 100, interval=10,
                             clock=lambda: now[0], sleep=sleep)
        api.kernels_push.assert_not_called()
        return result

    def test_polling_success_error_cancellation_unknown(self):
        self.assertEqual(self.wait_with_statuses(["QUEUED", "RUNNING", "COMPLETE"]), "COMPLETE")
        self.assertEqual(self.wait_with_statuses(["ERROR"]), "ERROR")
        self.assertEqual(self.wait_with_statuses(["CANCEL_ACKNOWLEDGED"]), "CANCEL_ACKNOWLEDGED")
        with self.assertRaises(launch.WorkflowError):
            self.wait_with_statuses(["UNEXPECTED"])

    def test_wait_timeout_does_not_resubmit(self):
        api = Mock()
        api.kernels_status.return_value = SimpleNamespace(status=SimpleNamespace(name="RUNNING"))
        now = [0.0]
        with self.assertRaises(launch.WaitExpired):
            launch.wait(api, "testuser/ntoma-outfits-test/1", 10, interval=30,
                        clock=lambda: now[0], sleep=lambda n: now.__setitem__(0, now[0] + n))
        self.assertEqual(now[0], 10)
        api.kernels_push.assert_not_called()

    def create_report(self):
        directory = self.root / "outputs/ntoma-outfits"
        directory.mkdir(parents=True)
        # Fixture bytes exercise artifact integrity, not a generated image or GPU test.
        fixture = b"fixture-for-checksum-verification"
        (directory / "outfit-01.png").write_bytes(fixture)
        images = [{"file": "outfit-01.png", "sha256": hashlib.sha256(fixture).hexdigest(),
                   "safety_blocked": False}]
        images.extend({"file": None, "sha256": None, "safety_blocked": True} for _ in range(7))
        report = {"completed": True, "custom_trained": False, "task": "outfit_concept_inference",
                  "safety_checker_enabled": True, "images": images}
        (directory / "index.html").write_text("<html>test</html>")
        path = directory / "run-report.json"
        launch.write_json(path, report)
        return directory, path, report

    def test_verify_outputs_and_tamper_detection(self):
        directory, path, report = self.create_report()
        self.assertEqual(launch.verify_outputs(self.root / "outputs"), directory / "index.html")
        (directory / "outfit-01.png").write_bytes(b"modified")
        with self.assertRaises(launch.WorkflowError):
            launch.verify_outputs(self.root / "outputs")

    def test_reject_incomplete_report_and_path_escape(self):
        directory, path, report = self.create_report()
        report["completed"] = False
        launch.write_json(path, report)
        with self.assertRaises(launch.WorkflowError):
            launch.verify_outputs(directory)
        report["completed"] = True
        report["images"][0]["file"] = "../../outside.png"
        launch.write_json(path, report)
        with self.assertRaises(launch.WorkflowError):
            launch.verify_outputs(directory)

    def test_download_tracks_specific_version_and_selected_files(self):
        api = Mock()
        launch.download(api, "testuser/ntoma-outfits-test/4", self.root)
        args, kwargs = api.kernels_output.call_args
        self.assertEqual(args[0], "testuser/ntoma-outfits-test/4")
        self.assertIn("run-report", kwargs["file_pattern"])
        self.assertIs(kwargs["quiet"], True)

    def test_gallery_escapes_text_and_handles_blocked_output(self):
        report = {"model_id": "test-model", "images": [
            {"title": "<script>bad</script>", "prompt": "<img>", "file": None, "seed": 1}]}
        html = outfit_job.gallery(report)
        self.assertNotIn("<script>", html)
        self.assertIn("&lt;script&gt;", html)
        self.assertIn("withheld", html)
        self.assertIn("not custom-trained", html)


if __name__ == "__main__":
    unittest.main()
