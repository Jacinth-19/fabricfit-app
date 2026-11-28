#!/usr/bin/env python3
"""One-off: fill the 203 English keys that have no translation in any pack.

These keys exist in values/strings.xml but were absent from the tw/gaa/ee TSV
packs, so generate.py emitted them as empty <string> elements and every one of
them rendered as a blank label on a Twi, Ga or Ewe device. This script appends
the translations to the packs; after it runs, generate.py lights up 203 keys per
language with no blanks left.

Format rules (enforced by generate.py on the next run):
  * %s / %d / %1$s / %2$s / %02d must match the English value exactly.
  * A backslash-n in the TSV becomes a real newline in the XML, matching how
    values/strings.xml encodes its two-line headlines.
  * Single quotes and double quotes are escaped by the generator, not here.
Brand feature names (FabricFit, FitScan, FitMatch, FitRoom, My Fit Closet) and
fabric names stay in English, per the localisation rule in HANDOVER.md.
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
I18N = ROOT / "FabricFit/tools/i18n"

# key: (twi, ga, ewe)
T = {
# ---------- onboarding / brand ----------
"onboarding_brand_badge": ("Ghana · Wɔyɛ ma wo", "Ghana · Ato nɛ ma bo", "Ghana · Wotsɔ wɔwɔ na wo"),
"onboarding_welcome_headline": ("Hyɛ ntoma a, wɔahyɛ ma wo.", "Mɔ́ɔ klɛŋ ni ake bo.", "Do nu si wòle be ye."),
"onboarding_welcome_sub": ("Hunu ntoma no. Hunu ntade no. Tɔ hyɛ no — kaba, agbada, kaftan ne nea ɛka ho, wɔayɛ ama wo.", "Na klɛŋ lɛ. Na atade lɛ. Hɔ shiŋ lɛ — kaba, agbada, kaftan kɛ ehe ni ekoŋŋ, ato nɛ ma bo.", "Kpɔ nu la. Kpɔ awu la. Ɖa si — kaba, agbada, kaftan kple nu bubu, wotsɔ wɔwɔ na wo."),
"onboarding_proof_offline": ("Ɛyɛ adwuma a intanɛt", "Enyɛɛ adwuma ni intanɛt", "Edzɔa dɔ le intanɛt manɔmee"),
"onboarding_proof_tryon": ("AI nhyɛso", "AI atade niɛ", "AI awu nukpɔkpɔ"),
"onboarding_proof_free": ("Ɛyɛ kwa", "Yɛɛ fee", "Emee"),
# ---------- tailoring options ----------
"custom_sleeve_three_quarter": ("Nsa mmiɛnsa", "Sɛɛ nu etɛ", "Nu etɔ̃"),
"custom_neck_boat": ("Ɔpaa kɔn", "Kɔŋ ŋtsɔɔ", "Kɔ boŋ"),
"custom_length_floor": ("Du asase", "Shɛ shikpɔŋ", "Ɖi anyigba"),
# ---------- fabric ----------
"fabric_wool": ("Wul / Nwira", "Wul / Nwira", "Wul / Nwira"),
"fabric_wool_desc": ("Nwira a wɔanwira, anaasɛ wɔayɛ no kɔtɔ.", "Nwira ni atsɛ̃ alo kɔtɔ.", "Wul alo kɔti si wotsɔ wɔwɔ."),
# ---------- empty states ----------
"empty_action_scan_fabric": ("Sɔ wo ntoma a ɛdi kan", "Kpɔ wo klɛŋ klɛŋ", "Kpɔ wò nu gbãtɔ"),
"empty_action_create_collection": ("Bɔ kuw bi", "Bɔ kuu ko", "Wɔ ƒuƒoƒo ɖeka"),
"empty_action_add_to_wardrobe": ("Fa ka wo ntade dan no ho", "Fa kɛ wo atade dan lɛ mli", "Tsɔ de wò awu dan la me"),
"empty_action_browse_designs": ("Hwɛ atade nhyehyɛe", "Kwɛ atade hewalɛ", "Kpɔ awu ɖoɖowo"),
"empty_action_start_exploring": ("Fi ase hwehwɛ", "Je mli ni okwɛ", "Dze ŋgɔ nukpɔkpɔ"),
"empty_action_try_design": ("Sɔ atade bi", "Kɛmɔ atade ko", "Te awu ɖeka"),
"empty_action_clear_filters": ("Yi nhunuhama no", "Je ninaa lɛ", "Ɖe nyɔɖenyɔɖewo ɖa"),
"empty_secondary_learn_more": ("Sua bi ka ho", "Kase bɔ ni ekoŋŋ", "Srɔ̃ nu geɖe"),
# ---------- accessibility ----------
"accessibility_title": ("Akwan a wɔfa so hwɛ adwuma", "Mɛi ni abɔ nɔ", "Ame ŋutɔ ƒe mɔnuwo"),
"accessibility_desc": ("Siesie app no ma wo ho ahiadɛ", "Saa app lɛ bo nɔɔ", "Trɔ app la ɖo wò nu"),
"accessibility_high_contrast": ("Ahwehwɛ kɛse", "Ninaa kɛ", "Nukpɔkpɔ kpekpeme"),
"accessibility_high_contrast_desc": ("Fa nsɛsɛm a ɛyɛ den ma wuhu nneɛma yiye", "Kɛmɔ amɛ ni ehiɛ nyam", "Zã ɔsiwo si dze ƒã nyuie"),
"accessibility_reduced_motion": ("Nnyigyei ketewaa", "Shishi bɔɔ", "Ɣeyiɣi kpui"),
"accessibility_reduced_motion_desc": ("Teɛ nneɛma a ɛkɔ so kɔ ne so no", "Ŋyɛ naa ni shishiɔ lɛ", "Ɖe agbɔsɔsɔwo dzi"),
"accessibility_large_text": ("Nkyerɛwee kɛse", "Ŋmaa kɛ", "Ŋɔŋlɔ gã"),
"accessibility_large_text_desc": ("Kɔ nkyerɛwee kɛse wɔ app no nyinaa mu", "Ŋmaa kɛ kɛ app lɛ fɛɛ mli", "Lɔ̃ ɔŋŋŋlɔ gã le app la me katã"),
"accessibility_haptic_feedback": ("Nnyigyei nka", "Shishi ni mɔɔ", "Asi ƒomedodo"),
"accessibility_haptic_feedback_desc": ("Wobɛte nnyigyei wɔ bere a woklik", "Le sɛɛ lɛ woonuɔ", "Anu le ne ƒom ɖe nu"),
# ---------- biometric / security ----------
"biometric_title": ("Bue FabricFit", "Kɛ FabricFit", "Ʋu FabricFit"),
"biometric_subtitle": ("Maa wonim wo nipaduam", "Hã ole bo diɛŋtsɛ", "Ðo wò ŋutɔ nyuie"),
"biometric_description": ("Fa wonsa anaa woyɛ mu bue wo ntade dan", "Kɛ owo alo ohiɛ kɛ kɛ o", "Zã wò asi alo wò mo nɔ ƒu wò awu dan"),
"biometric_available_strong": ("Wonsa anaa woyɛ mu wɔ hɔ", "Owo alo ohiɛ yɛ", "Wò asi alo wò mo li"),
"biometric_available_weak": ("Nipaduam nhwɛso wɔ hɔ", "Mɔɔ ni mɔɔ", "Ame ŋutɔ ƒe mɔnu li"),
"biometric_not_available": ("Nipaduam nhwɛso nni hɔ", "Mɔɔ ni mɔɔ bɛ", "Ame ŋutɔ ƒe mɔnu mele o"),
"security_title": ("Ahobammɔ", "Hilɔ", "Dedienɔnɔ"),
"security_biometric_lock": ("App no mu bue", "App lɛ mli kɛ", "App la ƒuƒu"),
"security_biometric_lock_desc": ("Hwehwɛ nipaduam ansa na wobebue app no", "Hã ole bo diɛŋtsɛ danŋ app lɛ", "Bia wò ŋutɔ nyaŋo hafi ʋu app la"),
"security_biometric_premium": ("Bɔ nneɛma a ɛyɛ dɛ no ho ban", "Bu nɔ ni yɛ nyam lɛ", "Srɔ̃ nu xɔasiwo ta"),
"security_biometric_premium_desc": ("Hwehwɛ nipaduam ma nneɛma a ɛyɛ dɛ nko", "Hã ole bo diɛŋtsɛ nɔ ni yɛ nyam lɛ pɛ", "Bia wò ŋutɔ nyaŋo le nu xɔasiwo ko ŋu"),
# ---------- share / export ----------
"share_title": ("Fa kɔ", "Fa ya", "Tsɔ yi"),
"share_look_text_with_fabric": ("Hwɛ %1$s a meyɛe wɔ FabricFit, fa %2$s ntoma! 🎨✨ #FabricFit #GhanaianFashion", "Kwɛ %1$s ni mifee yɛ FabricFit, kɛ %2$s klɛŋ! 🎨✨ #FabricFit #GhanaianFashion", "Kpɔ %1$s si mewɔ le FabricFit, kple %2$s nu! 🎨✨ #FabricFit #GhanaianFashion"),
"export_title": ("Fa wo ntade dan kɔ", "Fa o atade dan lɛ ya", "Tsɔ wò awu dan la yi"),
"export_pdf": ("Yɛ no PDF", "Fee no PDF", "Wɔe PDF"),
"export_pdf_desc": ("Yɛ wo ntoma ne wo ntade nhoma", "Fee o klɛŋ kɛ o atade wolo", "Wɔ wò nu kple wò awu agbale"),
"export_image": ("Yɛ no mfonini", "Fee no foto", "Wɔe nɔnɔmetata"),
"export_image_desc": ("Fa ntoma biara kɔ ne nsɛm ka ho", "Fa klɛŋ ɖeɖe kɛ ehe saji", "Tsɔ nu ɖeɖe kple eƒe nyawo yi"),
"export_success": ("Wɔayɛ ntade dan no nyɛ", "Ato atade dan lɛ jogbaŋŋ", "Wowɔ awu dan la nyuie"),
"export_failed": ("Ɛanyɛɛ yɛ", "Enyɛɛ fee", "Mewɔe o"),
"export_share": ("Fa deɛ wɔayɛ no kɔ", "Fa nɔ ni ato lɛ ya", "Tsɔ nu si wowɔ la yi"),
# ---------- filter / sort ----------
"action_filter": ("Yi", "Je", "Ɖe"),
"filter_title": ("Yi na na", "Je ni na", "Ɖe kple ɖoɖo"),
"filter_sort_by": ("Yi no", "Je nɔ", "Ðo ɖoɖo"),
"filter_category": ("Kuw", "Kuu", "Ƒuƒoƒo"),
"filter_all_categories": ("Kuw nyinaa", "Kuu fɛɛ", "Ƒuƒoƒo katã"),
"sort_newest": ("Foforɔ di kan", "Nɔ ni yɔɔ lɛ klɛŋ", "Yeyea gbã"),
"sort_oldest": ("Dada di kan", "Nɔ ni bɔ lɛ klɛŋ", "Xoxoa gbã"),
"sort_name_az": ("Din (A-Z)", "Gbɛi (A-Z)", "Ŋkɔ (A-Z)"),
"sort_name_za": ("Din (Z-A)", "Gbɛi (Z-A)", "Ŋkɔ (Z-A)"),
# ---------- community ----------
"community_title": ("Kurowm", "Kuu", "Habɔbɔ"),
"community_empty_title": ("Nnwuma nni hɔ", "Saji bɛ", "Nuwo mele o"),
"community_empty_body": ("Di kan fa woasa kyerɛ kurowm no!", "Klɛŋ fa o hewalɛ kɛ tsɔɔ kuu lɛ!", "Ŋgɔ gbã tsɔ wò nu fia habɔbɔ la!"),
"community_empty_action": ("Yɛ wo nnwuma a ɛdi kan", "Fee o saji klɛŋ", "Wɔ wò nu gbãtɔ"),
"community_active_challenges": ("Akansi a ɛkɔ so", "Hewalɛ ni yaa nɔ", "Nu Kpɔɖeŋu siwo le edzi"),
"community_gallery": ("Atade mfonini dan", "Atade foto dan", "Awu nɔnɔmetata dan"),
"community_following": ("Wodi wo akyi", "Mɛi ni nyiɛ o sɛɛ", "Amesiwo le wò yome"),
"community_followers": ("Wodidi wo akyi", "Mɛi ni nyiɛ o sɛɛ", "Amesiwo kplɔ wò"),
"community_posts": ("Nnwuma", "Saji", "Nuwo"),
"action_like": ("Pɛ", "Sɔ", "Lɔ̃"),
"action_comment": ("Ka nsɛm", "Kɛ saji", "Ŋlɔ nya"),
"action_follow": ("Di akyi", "Nyiɛ sɛɛ", "Kplɔ"),
"action_unfollow": ("Gyae akyidi", "Ŋyɛ nyiɛ sɛɛ", "Ɖe kplɔdɔdzi"),
"challenge_participants": ("Mpanyinfo %d", "Mɛi %d", "Ame %d"),
"challenge_join": ("Ka akansi no ho", "Ba hewalɛ lɛ mli", "Ge ɖe nu kpɔɖeŋu la me"),
"challenge_submit": ("Fa wo adwuma", "Hɔ o nɔ", "Ðo wò nu"),
"challenge_ends": ("Ɛkɔ so kosi %s", "Ebaa yaa nɔ kɛ %s", "Ewɔa yi %s"),
# ---------- user profile / gallery ----------
"user_profile": ("Wo ho nsɛm", "O he saji", "Wò ŋutɔ nyawo"),
"gallery_detail": ("Atade nsɛm", "Atade saji", "Awu nyawo"),
"add_comment": ("Ka nsɛm…", "Kɛ saji…", "Ŋlɔ nya…"),
"post_comment": ("Fa gu so", "Hɔ", "Ðo ɖa"),
"comments_count": ("Nsɛm %d", "Saji %d", "Nya %d"),
"likes_count": ("Pɛ %d", "Sɔ %d", "Lɔ̃ %d"),
"fabrics_used": ("Ntoma a wɔfa", "Klɛŋ ni akɛ", "Nu si wozã"),
"style_used": ("Atade a wɔfa", "Atade ni akɛ", "Awu si wozã"),
"user_profile_not_found": ("Wonhu nyɔho no", "Ana nyɛ nyɛ", "Mekpɔ ame la o"),
# ---------- search ----------
"search_hint": ("Hwehwɛ nnwuma, nyɔho, tag…", "Tao saji, mɛi, tag…", "Di nuwo, amewo, tag…"),
"search_recent": ("Nhwehwɛ a etwa mu", "Nɔ ni atao yɛ maŋ", "Nukpɔkpɔ si va yi"),
"search_no_recent": ("Nhwehwɛ biara nni hɔ", "Nɔ ni atao bɛ", "Nukpɔkpɔ a va yi meli o"),
"search_no_results": ("Wonhu biara", "Ana bɛ", "Mekpɔ naneke o"),
"action_clear_all": ("Yi nyinaa", "Je fɛɛ", "Ɖe katã"),
"filter_date_range": ("Bere a ɛwɔ mu", "Gbɛ ni yɔɔ mli", "Ɣeyiɣi dometsotso"),
"filter_tags": ("Tags", "Tags", "Tags"),
# ---------- edit / image ----------
"action_edit": ("Siesie", "Tsakemɔ", "Trɔ"),
"image_aspect_ratio": ("Mfonini kɛseɛ", "Foto kɛ", "Nɔnɔmetata ƒe agbɔsɔsɔ"),
"image_edit_title": ("Siesie mfonini", "Tsake foto", "Trɔ nɔnɔmetata"),
"image_crop": ("Twa", "Tɛ", "Lã"),
"image_rotate": ("Dane", "Tsɛ", "Trɔ"),
"image_reset": ("San fi ase", "Ku sɛɛ", "Gbugbɔ"),
# ---------- report ----------
"action_report": ("Ka asɛm", "Kɛ saji", "Ƒo nu"),
"report_title": ("Ka asɛm wɔ emu", "Kɛ saji yɛ nɔ ni", "Ƒo nu le nu si ŋu"),
"report_description": ("Boa yɛn ma yɛte asɛm no ase: yi tirimpɔ a woaka no.", "Ye bua wɔ ni wɔnu saji lɛ: je nɔ hewɔ ni ooma lɛ.", "Kpe ɖe míaŋ be míase nya la: tia susu si ta wòƒo nu la."),
"report_select_type": ("Yi tirimpɔ", "Je nɔ hewɔ", "Tia susu"),
"report_additional_info": ("Nsɛm foforɔ (sɛ wopɛ)", "Saji kroŋŋ (kɛ osumɔɔ)", "Nya bubuwo (ne èdi be)"),
"report_placeholder": ("Ka nsɛm pii fa asɛm no ho…", "Kɛ saji babaoo yɛ saji lɛ he…", "Ŋlɔ nya geɖewo ku ɖe nya la ŋu…"),
"report_submit": ("Fa asɛm no", "Hɔ saji lɛ", "Ðo nya la"),
"report_submitting": ("Ɛrekɔ…", "Eyaa nɔ…", "Ele yiyim…"),
"report_success": ("Wɔafa asɛm no yie", "Ahɔ saji lɛ jogbaŋŋ", "Woɖo nya la nyuie"),
"report_already_reported": ("Woaka asɛm wɔ emu dedaw", "Okɛ saji yɛ nɔ ni dedaw", "Èƒo nu ɖe nu sia ŋu xoxo"),
"report_type_spam": ("Nkontompo anaa nsɛm hunu", "Saji ni mɔɔ bɛ alo lakamɔ", "Nya dzodzro alo aɖaŋu"),
"report_type_inappropriate": ("Nneɛma a ɛnsɛ", "Nɔ ni esa akɛ", "Nu si mesɔ o"),
"report_type_harassment": ("Tɔ̃tɔ̃mɔ anaa adɛn", "Hiao alo gbeye", "Ƒoɖoɖo alo ŋɔŋlɔɖi"),
"report_type_copyright": ("Copyright a wobu", "Copyright ni agbɔ", "Copyright ƒe kpɔɖeŋu"),
"report_type_misleading": ("Nsɛm a ɛdaadaa", "Saji ni lakamɔ", "Nya si blea ame"),
"report_type_other": ("Nea ɛka ho", "Nɔ kroŋŋ", "Nu bubu"),
# ---------- rich text ----------
"format_bold": ("Den", "Kɛ", "Dzẽ"),
"format_italic": ("Tɛtrɛɛ", "Tsɛ̃", "Ɖeɖe"),
"format_code": ("Koodu", "Koodu", "Koodu"),
"format_link": ("Link", "Link", "Link"),
"format_quote": ("Nsɛm a wɔaka", "Saji ni akɛ", "Nya si woƒo"),
"format_list": ("Nnɔtɛ", "Nɔŋ nɔ", "Ŋkɔŋlɔ"),
"richtext_helper": ("Ɛboa markdown: **den**, *tɛtrɛɛ*, `koodu`, [link](url), > nsɛm a wɔaka, - nnɔtɛ", "Eboa markdown: **kɛ**, *tsɛ̃*, `koodu`, [link](url), > saji ni akɛ, - nɔŋ nɔ", "Ekpena markdown: **dzẽ**, *ɖeɖe*, `koodu`, [link](url), > nya si woƒo, - ŋkɔŋlɔ"),
"description_label": ("Nkyerɛmu", "Saji", "Nyaɖeɖefia"),
"description_placeholder": ("Ka nsɛm fa woasa ho… (markdown wɔ hɔ)", "Kɛ saji yɛ o hewalɛ he… (markdown yɛ)", "Ŋlɔ nya ku ɖe wò nu ŋu… (markdown le)"),
# ---------- notifications ----------
"notification_preferences_title": ("Amanneɛbɔ nhyehyɛe", "Sane ni abɔ", "Nyatakaka ƒe ɖoɖowo"),
"notification_category_social": ("Nipa a wɔne bɔ", "Mɛi ni a kɛ", "Ame kple ame ƒe nuwɔna"),
"notification_category_community": ("Kurowm", "Kuu", "Habɔbɔ"),
"notification_category_other": ("Nea ɛka ho", "Nɔ kroŋŋ", "Nu bubu"),
"notification_pref_likes": ("Pɛ", "Sɔ", "Lɔ̃"),
"notification_pref_likes_desc": ("Sɛ obi pɛ wo nnwuma", "Kɛ mɔ ko sɔ o saji", "Ne ame aɖe lɔ̃ wò nu"),
"notification_pref_comments": ("Nsɛm", "Saji", "Nyawo"),
"notification_pref_comments_desc": ("Sɛ obi ka nsɛm wɔ wo nnwuma ho", "Kɛ mɔ ko kɛ saji yɛ o saji nɔ", "Ne ame aɖe ŋlɔ nya ɖe wò nu ŋu"),
"notification_pref_follows": ("Wodidi wo akyi foforɔ", "Mɛi ni nyiɛ o sɛɛ yɔɔ", "Amesiwo kplɔ wò yeyewo"),
"notification_pref_follows_desc": ("Sɛ obi di wo akyi", "Kɛ mɔ ko nyiɛ o sɛɛ", "Ne ame aɖe kplɔ wò"),
"notification_pref_mentions": ("Wɔaka wo din", "Atsɛ o gbɛi", "Woyɔ wò ŋkɔ"),
"notification_pref_mentions_desc": ("Sɛ obi ka wo din", "Kɛ mɔ ko tsɛ o gbɛi", "Ne ame aɖe yɔ wò ŋkɔ"),
"notification_pref_challenges": ("Akansi", "Hewalɛ", "Nu Kpɔɖeŋuwo"),
"notification_pref_challenges_desc": ("Nsɛm fa akansi ho", "Saji yɛ hewalɛ he", "Nya ku ɖe nu kpɔɖeŋuwo ŋu"),
"notification_pref_email": ("Email amanneɛbɔ", "Email sane", "Email nyatakaka"),
"notification_pref_email_desc": ("Nya amanneɛbɔ wɔ email so", "Ná sane yɛ email nɔ", "Xɔ nyatakaka le email dzi"),
"notification_preferences_info": ("Wɔkora nsakrae nyinaa. Amanneɛbɔ a ɛbɛba daa hwehwɛ intanɛt.", "Ato tsakemɔ lɛ fɛɛ. Sane ni ba daa baa intanɛt.", "Woxenaa tɔtrɔwo katã. Nyatakaka si gbɔna biaa intanɛt."),
"notification_like_title": ("%s pɛ wo nnwuma", "%s sɔ o saji", "%s lɔ̃ wò nu"),
"notification_like_body": ("Hwɛ obi a ɔpɛ woasa no", "Kwɛ mɔ ni sɔ o hewalɛ lɛ", "Kpɔ ame si lɔ̃ wò nu la"),
"notification_comment_title": ("%s ka nsɛm wɔ wo nnwuma ho", "%s kɛ saji yɛ o saji nɔ", "%s ŋlɔ nya ɖe wò nu ŋu"),
"notification_comment_body": ("Hwɛ deɛ ɔka fa woasa ho", "Kwɛ nɔ ni ekɛ yɛ o hewalɛ he", "Kpɔ nu si wògblɔ ku ɖe wò nu ŋu"),
"notification_follow_title": ("%s fi ase didi wo akyi", "%s je ni nyiɛ o sɛɛ", "%s dze ŋgɔ kplɔ wò"),
"notification_follow_body": ("Wowɔ obi a ɔdi wo akyi foforɔ!", "Owo mɔ kroŋŋ ni nyiɛ o sɛɛ!", "Wò ame yeye a kplɔ wò!"),
"notification_challenge_title": ("Akansi nsɛm foforɔ", "Hewalɛ saji kroŋŋ", "Nu Kpɔɖeŋu nya yeye"),
"notification_challenge_body": ("Nneɛma foforɔ wɔ %s mu", "Nɔ kroŋŋ yɛ %s mli", "Nu yeye le %s me"),
"channel_likes": ("Pɛ", "Sɔ", "Lɔ̃"),
"channel_likes_description": ("Amanneɛbɔ bere a obi pɛ wo nnwuma", "Sane kɛ mɔ ko sɔ o saji", "Nyatakaka ne ame aɖe lɔ̃ wò nu"),
"channel_comments": ("Nsɛm", "Saji", "Nyawo"),
"channel_comments_description": ("Amanneɛbɔ bere a obi ka nsɛm wɔ wo nnwuma ho", "Sane kɛ mɔ ko kɛ saji yɛ o saji nɔ", "Nyatakaka ne ame aɖe ŋlɔ nya ɖe wò nu ŋu"),
"channel_follows": ("Wodidi wo akyi", "Mɛi ni nyiɛ o sɛɛ", "Amesiwo kplɔ wò"),
"channel_follows_description": ("Amanneɛbɔ bere a obi di wo akyi", "Sane kɛ mɔ ko nyiɛ o sɛɛ", "Nyatakaka ne ame aɖe kplɔ wò"),
"channel_challenges": ("Akansi", "Hewalɛ", "Nu Kpɔɖeŋuwo"),
"channel_challenges_description": ("Amanneɛbɔ fa akansi ne akansi ahorow ho", "Sane yɛ hewalɛ kɛ hewalɛ kroŋŋ he", "Nyatakaka ku ɖe nu kpɔɖeŋuwo ŋu"),
# ---------- image filters ----------
"image_filters_title": ("Mfonini nhyehyɛe", "Foto hewalɛ", "Nɔnɔmetata ƒe ɖoɖowo"),
"filter_brightness": ("Hann", "Kpaŋ", "Kekeli"),
"filter_contrast": ("Nsonso", "Gbɔɔ", "Vovototo"),
"filter_saturation": ("Kɔla dɛ", "Mlɛ", "Amadede ƒe ɖiɖi"),
"filter_reset": ("San fi ase", "Ku sɛɛ", "Gbugbɔ"),
# ---------- media / video ----------
"media_type_image": ("Mfonini", "Foto", "Nɔnɔmetata"),
"media_type_video": ("Video", "Video", "Video"),
"video_select": ("Yi video", "Je video", "Tia video"),
"video_processing": ("Yɛ video no...", "Fee video lɛ...", "Wɔ video la..."),
"video_compressing": ("Ɛreka video no so...", "Ɛ̃ video lɛ...", "Wokɔa video la ɖe anyi..."),
"video_generating_thumbnail": ("Ɛreyɛ mfonini ketewaa...", "Efee foto bibioo...", "Wɔa nɔnɔmetata sue..."),
"video_duration_format": ("%d:%02d", "%d:%02d", "%d:%02d"),
"video_trim": ("Twa video no", "Tɛ video lɛ", "Lã video la"),
"video_trim_start": ("Bere a wofi ase", "Gbɛ ni je mli", "Ɣeyiɣi si dze ŋgɔ"),
"video_trim_end": ("Bere a egyae", "Gbɛ ni gbɛ", "Ɣeyiɣi si wu enu"),
"video_preview": ("Hwɛ ansa", "Kwɛ klɛŋ", "Kpɔ do ŋgɔ"),
"video_upload": ("Fa video no", "Hɔ video lɛ", "Ðo video la ɖe dzi"),
"video_trim_title": ("Twa video no", "Tɛ video lɛ", "Lã video la"),
"video_trim_duration": ("Bere tenten: %s", "Gbɛ teŋ: %s", "Ɣeyiɣi ƒe didime: %s"),
"video_trim_reset": ("San fi ase", "Ku sɛɛ", "Gbugbɔ"),
"video_trim_apply": ("Fa ntwa no", "Hɔ tɛɛmɔ lɛ", "Zã lilã la"),
# ---------- blocked users ----------
"blocked_users_empty": ("Nyɔho a wɔasiw biara nni hɔ", "Mɛi ni atsi bɛ", "Ame siwo woxe mɔ ɖo meli o"),
"action_unblock": ("Bue kwan", "Híɛ kwan", "Ʋu mɔ"),
"unblock_confirm_title": ("Bue kwan ma nyɔho no", "Híɛ kwan ma mɔ lɛ", "Ʋu mɔ na ame la"),
"unblock_confirm_message": ("Wopɛ sɛ wobue kwan ma %s?", "Osumɔɔ ni ohiɛ kwan ma %s?", "Èdi be yeaʋu mɔ na %sa?"),
"action_block": ("Siw kwan", "Tsi kwan", "Xe mɔ"),
"block_confirm_title": ("Siw kwan ma nyɔho no", "Tsi kwan ma mɔ lɛ", "Xe mɔ na ame la"),
"block_confirm_message": ("Wopɛ sɛ wosiw kwan ma %s? Wɔrennya wo bio.", "Osumɔɔ ni otsi kwan ma %s? Ebaanyɛ ekɛ bo awo nii dɔŋŋ.", "Èdi be yeaxe mɔ na %sa? Mava te ŋu awɔ nu kple wò o."),
# ---------- admin ----------
"admin_moderation_title": ("Nhwehwɛmu dan", "Kwɛmɔ dan", "Dzodzro dan"),
"admin_no_reports": ("Asɛm a wɔaka biara nni hɔ", "Saji ni akɛ bɛ", "Nya siwo woƒo meli o"),
"admin_dismiss": ("Gyae", "Je", "Ɖe ɖa"),
"admin_take_action": ("Fa nneyɛɛ", "Wɔ nɔ", "Wɔ nu"),
"report_details_reported_user": ("Nyɔho a wɔkaa ne ho asɛm", "Mɔ ni akɛ e saji", "Ame si woƒo nu ɖe eŋu"),
"report_details_content_type": ("Nneɛma su", "Nɔ su", "Nu ƒe tɔtrɔ"),
"report_details_description": ("Nkyerɛmu", "Saji", "Nyaɖeɖefia"),
"report_details_date": ("Da a wɔkaa asɛm no", "Da ni akɛ saji lɛ", "Ŋkeke si woƒo nu la"),
"report_details_status": ("Tebea a ɛwɔ", "Teŋ", "Nɔnɔme"),
"settings_section_admin": ("Admin", "Admin", "Admin"),
"admin_moderation_dashboard": ("Nhwehwɛmu dan", "Kwɛmɔ dan", "Dzodzro dan"),
"admin_moderation_desc": ("Hwɛ asɛm a wɔaka no", "Kwɛ saji ni akɛ lɛ", "Dzro nya siwo woƒo la"),
# ---------- home hero ----------
"home_hero_headline": ("Ghana ntoma,\\nasetie mu nsusuw", "Ghana klɛŋ,\\nhe ni yɔɔ nyam", "Ghana nu,\\nawu yeye"),
"home_hero_subhead": ("Hunu kente, ankara ne lace a, yɛbɔ wo ho.", "Na kente, ankara kɛ lace ni abɔ bo.", "Kpɔ kente, ankara kple lace siwo wowɔ na wò."),
# ---------- recommender ----------
"recs_ranker_ondevice": ("Wɔakyekyere wɔ saa fɔn yi so - ɛyɛ adwuma a intanɛt", "Ato nɔ yɛ fon nɛ nɔ - enyɛɛ adwuma ni intanɛt", "Woɖo ɖoɖo le fon sia dzi - edzɔa dɔ le intanɛt manɔmee"),
"recs_ranker_cloud": ("Wo ntɔkwa adwuma no na ɛkyerɛ", "O sɛɛvɛs lɛ nɔŋ nɔ kɛmɔ", "Wò dɔwɔƒe la ɖo ɖoɖo"),
"recs_ranker_cloud_failed": ("Ntɔkwa adwuma no nni hɔ - yɛde deɛ ɛwɔ fɔn no so", "Sɛɛvɛs lɛ bɛ - yɛkɛ nɔ ni yɔɔ fon lɛ nɔ", "Dɔwɔƒe la meli o - míezã nu si le fon la dzi"),
"post_video_optimised": ("Wɔasiesie ama upload", "Ato ni ahɔ", "Wotrɔe na ɖoɖo"),
"post_video_original": ("Fael a ɛwɔ hɔ — wontumi nsiesie", "Fail ni yɔɔ — anyɛɛ ato", "Faɛl si le — mate ŋu atrɔe o"),
}


def main():
    # guard: every English key with no pack entry must be covered
    import re
    values = ROOT / "FabricFit/app/src/main/res/values/strings.xml"
    en = [k for k, _ in re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>',
                                   values.read_text(encoding="utf-8"), re.S)]
    have = set()
    for f in I18N.glob("*.tsv"):
        for line in f.read_text(encoding="utf-8").splitlines():
            if line.strip() and not line.startswith("#"):
                have.add(line.split("\t")[0].strip())
    need = [k for k in en if k not in have]
    lacking = [k for k in need if k not in T]
    print(f"English={len(en)} pack={len(have)} need={len(need)} translated={len(T)} lacking={len(lacking)}")
    if lacking:
        print("LACKING:", lacking)
        raise SystemExit(2)

    for idx, lang in enumerate(("tw", "gaa", "ee")):
        p = I18N / f"{lang}_b.tsv"
        text = p.read_text(encoding="utf-8")
        if not text.endswith("\n"):
            text += "\n"
        added = 0
        for k in need:
            if f"\n{k}\t" in "\n" + text:
                continue
            text += f"{k}\t{T[k][idx]}\n"
            added += 1
        p.write_text(text, encoding="utf-8")
        print(f"[{lang}] appended {added}")


if __name__ == "__main__":
    main()
