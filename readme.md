# Wiretap

A server-side Simple Voice Chat addon mod for Fabric, that adds a microphone and a speaker block.

## Disclaimer

When installing this on your server,
you are responsible for making sure that players are aware of the functionality of the mod.
This mod is not intended to be used for spying on people, this is **only for role-playing purposes**!

## Usage

- Install this mod alongside [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) on your server
- Make sure you set up the voice chat mod correctly (see [here](https://modrepo.de/minecraft/voicechat/wiki/server_setup))
- Craft a *Calibrated Skulk Sensor*
- Make sure you have at least one level if you are in survival mode
- Place the *Calibrated Skulk Sensor* in an anvil
- Rename the *Calibrated Skulk Sensor* to `wiretap` (not case-sensitive)
- Take out the microphone block
- When taking out the microphone, you will get a speaker added to your inventory as well
- Place the microphone block where you want to listen in on people
- Place the speaker block where you want to hear people
- The microphone and speaker blocks will have a unique ID shown in their tooltip - Microphones and speakers with the same ID will be connected to each other
- Don't place multiple microphones or speakers with the same ID, as this will cause issues

## Commands

- `/wiretap items [speaker_radius]` - Gives you a microphone and speaker (Requires OP permissions by default)
 - If a radius isn't provided, the server speaker range is used by default.

## Configuration

| Property                   | Description                                                                                       | Default       |
|----------------------------|---------------------------------------------------------------------------------------------------|---------------|
| `microphone_pickup_range`  | The range in which microphones can pick up sounds                                                 | `32.0`        |
| `speaker_audio_range`      | The audible range of speakers                                                                     | `32.0`        |
| `command_permission_level` | The permission level required to use the commands                                                 | `2`           |
| `packet_buffer_size`       | The amount of packets to buffer before playing                                                    | `6`           |
| `anvil_crafting`           | Whether the items can be crafted in the anvil                                                     | `true`        |
| `speaker_sound_effect`     | The sound effect to apply to the speaker - Valid values are: `none`, `old_speaker`, `low_quality` | `old_speaker` |
| `microphone_skin_url`      | The skin url for the microphone block                                                             |               |
| `speaker_skin_url`         | The skin url for the speaker block                                                                |               |

## Credits

- [Microphone Skin](https://minecraft-heads.com/custom-heads/decoration/6360-studio-microphone)
- [Speaker Skin](https://minecraft-heads.com/custom-heads/decoration/215-radio)
