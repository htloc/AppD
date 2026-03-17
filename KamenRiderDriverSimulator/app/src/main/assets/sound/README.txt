SOUND ASSETS
============
Place OGG/MP3 sound files here. File names must match the IDs defined in
GameConfig.Sounds (all lowercase, underscores).

Expected files:
  exaid_henshin.ogg           - Ex-Aid henshin voice
  level_up.ogg                - Level-up jingle
  gashat_insert.ogg           - Gashat insertion click
  belt_standby.ogg            - Belt standby loop
  mighty_action_x_jingle.ogg  - Mighty Action X game jingle
  rider_kick.ogg              - Rider Kick SFX
  critical_strike.ogg         - Critical Strike SFX
  game_clear.ogg              - Game clear fanfare
  menu_select.ogg             - Menu selection click
  menu_back.ogg               - Menu back click

These are loaded at runtime via SoundManager → res/raw/<name>.<ext>
(alternatively: place in res/raw/ and they will be found automatically).
