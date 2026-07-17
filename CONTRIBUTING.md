# Contributing to Wurst

This guide exists to save both sides time. ![](https://img.wimods.net/github.com/Wurst-Imperium/Wurst7/CONTRIBUTING.md)

## Priorities

1. **Maintainers first:** architecture, gametests, release workflows, review burden.
2. **Users second:** features, bugfixes, compatibility, documentation.
3. **Sponsors third:** advertising, donation perks, monetization.

Happy maintainers ensure happy users, happy users ensure happy sponsors. In that order.

## Issues

- **Search before posting:** Both GitHub issues and [WurstForum](https://wurstforum.net/) might have a post about your bug or feature request already. Find and read that first. Remember that posting duplicates makes it harder for the next person to find the original with all the answers.

- **State the bug or request clearly:** For requests, explain what it is, how it works, and why it matters. For bugs, include repro steps. "idk it's weird sometimes" is not actionable.

- **Include the crash report file:** If the game is crashing and/or it's unclear which versions you're using, I _need_ that file to diagnose the issue. No crash? Hold down <kbd>F3</kbd> + <kbd>C</kbd> for 10 seconds to force one.
  - Windows: `%APPDATA%/.minecraft/crash-reports/crash-<timestamp>-client.txt`
  - Linux: `~/.minecraft/crash-reports/crash-<timestamp>-client.txt`
  - macOS: `~/Library/Application Support/minecraft/crash-reports/crash-<timestamp>-client.txt`

- **If you want to implement the change yourself, say so.**

## Pull Requests

- **Communicate first:** Please talk to me before opening a PR. I don't like receiving PRs out of the blue. This is supposed to be a collaborative process.

- **1 PR = 1 change:** Each pull request should address a single issue or add a single feature. Don't stuff PRs with unrelated changes.

- **Make it easy to review:** Keep the diff small and the code readable. Include any `/give` commands or whatever I need to test your change in the PR description. Remember that review is the bottleneck.

- **Follow the code style:** Run `./gradlew spotlessApply` or Eclipse settings from the [codestyle folder](codestyle). For anything that this doesn't cover, please try to match the existing style as closely as possible.

- **Run tests:** `./gradlew check runClientGameTest runClientGameTestWithMods` must pass. Consider adding a gametest for your change. It's not easy, but it helps a lot.

- **Sign the CLA:** <https://cla-assistant.io/Wurst-Imperium/Wurst7>. See below for why.

### Why we have a CLA

The CLA basically just restates the important bits of the GPLv3 license:

- Copyright exists. You can't just submit code that you don't have the rights to.
- You can't request to have your PR "un-merged" later. No takesies-backsies.
- Anyone can use Wurst, even if it includes your code, to do things you don't like.
- Anyone can sell or make money from Wurst, even if it includes your code, without having to pay you.

The GPL already enforces these rules. The CLA is just there to draw attention to them, because "I didn't read the license" has happened enough times already.

## Wurst Wiki

Anyone can contribute to the wiki. You don't need permission. You only need an email address to create an account.

See: <https://wurst.wiki/how_to_help>

Note: The wiki has a Cloudflare captcha on the login/registration page to lock out spam bots. Sorry if this is blocked in your country. It's necessary.
