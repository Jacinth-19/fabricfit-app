"""Narrow Kaggle adapter; imports/authentication happen only for a remote action.

Kaggle 2.2.4 accepts a /version argument but ignores it for status/output. Use the
SDK version_label explicitly so a later manual rerun cannot replace our results.
Never dump SDK responses or signed download URLs into local logs.
"""
from __future__ import annotations

from pathlib import Path, PurePosixPath
import re
from urllib.parse import urlsplit


class VersionedKaggle:
    def __init__(self, api):
        self.api = api

    def kernels_push(self, folder, timeout, acc):
        return self.api.kernels_push(folder, timeout=timeout, acc=acc)

    @staticmethod
    def fill_request(request, ref):
        owner, slug, version = ref.split("/")
        if not version.isdigit() or int(version) < 1:
            raise ValueError("Expected an explicit positive kernel version")
        request.user_name = owner
        request.kernel_slug = slug
        request.version_label = f"v{version}"
        return request

    def kernels_status(self, ref):
        from kagglesdk.kernels.types.kernels_api_service import ApiGetKernelSessionStatusRequest
        request = self.fill_request(ApiGetKernelSessionStatusRequest(), ref)
        with self.api.build_kaggle_client() as client:
            return client.kernels.kernels_api_client.get_kernel_session_status(request)

    def kernels_output(self, ref, path, *, file_pattern, force, quiet):
        import requests
        from kagglesdk.kernels.types.kernels_api_service import ApiListKernelSessionOutputRequest
        root = Path(path).resolve()
        root.mkdir(parents=True, exist_ok=True)
        token = None
        seen_tokens = set()
        files = []
        total_bytes = 0
        with self.api.build_kaggle_client() as client:
            while True:
                request = self.fill_request(ApiListKernelSessionOutputRequest(), ref)
                request.page_size = 100
                if token:
                    request.page_token = token
                response = client.kernels.kernels_api_client.list_kernel_session_output(request)
                for item in response.files or []:
                    name = item.file_name
                    if not re.search(file_pattern, name):
                        continue
                    relative = PurePosixPath(name)
                    if relative.is_absolute() or ".." in relative.parts or "\\" in name:
                        raise ValueError("Unsafe remote output path")
                    target = root.joinpath(*relative.parts)
                    if not target.resolve().is_relative_to(root) or target.is_symlink():
                        raise ValueError("Output path escapes download directory")
                    if urlsplit(item.url).scheme != "https":
                        raise ValueError("Expected HTTPS output download")
                    if target.exists() and not force:
                        raise FileExistsError(target)
                    if len(files) >= 16:
                        raise ValueError("Too many output files for this job")
                    target.parent.mkdir(parents=True, exist_ok=True)
                    # Do not use the authenticated Kaggle transport for signed storage
                    # URLs: the download request must carry no Kaggle bearer token.
                    with requests.Session() as session:
                        session.trust_env = False  # no .netrc credentials on storage requests
                        with session.get(item.url, stream=True, timeout=(15, 120)) as download:
                            download.raise_for_status()
                            with target.open("wb") as output:
                                for chunk in download.iter_content(chunk_size=64 * 1024):
                                    total_bytes += len(chunk)
                                    if total_bytes > 32 * 1024 * 1024:
                                        raise ValueError("Output download exceeded 32 MiB limit")
                                    output.write(chunk)
                    files.append(str(target))
                token = response.next_page_token
                if not token:
                    break
                if token in seen_tokens or len(seen_tokens) >= 10:
                    raise ValueError("Unexpected output pagination")
                seen_tokens.add(token)
        return files
