# Profile card templates

A profile card is an SVG file with a few placeholder tokens in it. The backend
serves it at `/api/overlay/profile/<file>?teamId=<id>`, replacing the tokens with
data for that team; the script embedded in the SVG then draws the card from that
data. The templates shipped with this repository are `config/_media/team.svg`
(team cards, ICPC-style history scene) and `config/_media/personal.svg` (single
person). Setup and reconciliation rules live in
[advanced.json.md](advanced.json.md#team-profile-cards); this page describes the
template format, which is a stable contract you can write your own templates
against.

## Substitution tokens

The backend replaces these four tokens, and only these four:

| Token           | Replaced with                                                                   |
|-----------------|---------------------------------------------------------------------------------|
| `{team.json}`   | the team record — the JSON document described below                              |
| `{render.json}` | the contents of `settings.json`, minus keys you did not set                      |
| `{mainColor}`   | the team's `color` from the contest system, if it has one                        |
| `{fontColor}`   | `fontColor` from `settings.json`, if set                                         |

Every occurrence of a token is replaced (the shipped templates contain each of
them exactly once), and a token whose data is absent is **left in the output
verbatim** — that is how the templates detect "no data" (see the fallback chain
below). `{mainColor}` and `{fontColor}` are
substituted before `{render.json}`, and `{team.json}` last, so a token literal
that happens to occur inside team data is never mistaken for a token.

`{Logo}`, `{LogoExtension}` and `{Background}` are **not** substituted by the
backend. They live inside `href="data:image/{LogoExtension};base64,{Logo}"`-style
attributes and are replaced by the template's own script, from `logo`,
`logoExtension` and `background` in `render.json` or from the built-in defaults.

## Escaping

`{team.json}` and `{render.json}` are substituted into `<![CDATA[...]]>` script
blocks. Before substitution, every `<` and `>` in the JSON is rewritten as the
JSON unicode escapes `\u003c` / `\u003e`. This keeps the blob free of `]]>` and of
anything an XML parser could read as markup, while `JSON.parse` turns the escapes
back into the original characters. Nothing else is escaped, so a template must
place these tokens inside CDATA and nowhere else.

`{mainColor}` and `{fontColor}` end up in a `<style>` block. Their values are
validated (`#` plus 3–8 hex digits) and dropped otherwise, so they can never
close the style block.

## The team record (`{team.json}`)

```json
{
  "id": "42",
  "university": {
    "fullName": "Sample University of Technology",
    "shortName": "SUT",
    "region": "Sample Region",
    "hashTag": "#SAMPLE",
    "appYears": [2018, 2019, 2021], "winYears": [2022],
    "goldYears": [2021], "silverYears": [2019], "bronzeYears": [2018],
    "regYears": [2018, 2019]
  },
  "team": { "name": "Sample Team", "regionals": ["3rd place, Sample Regional 2025"] },
  "coach": { "name": "Sample Coach", "cfRating": 2401, "achievements": [] },
  "contestants": [
    {
      "name": "Alice Sample",
      "altNames": ["A. Sample"],
      "cfHandle": "alice",
      "cfRating": 3216,
      "achievements": [
        { "achievement": "ICPC World Finals Gold Medalist (2024)", "priority": 104 }
      ]
    }
  ]
}
```

The World-Finals year lists are flattened onto `university` rather than nested.
`achievements` are objects with `achievement` (the text) and `priority` (higher
first). `coach` may be `null`. Every field is optional as far as the templates are
concerned — they render an empty string or drop the element when a field is
missing.

Two fields are consumed outside the drawing code: `altNames` is used by the
backend to match a profile person against the contest system's spelling of their
name and is not drawn; `cfHandle` is drawn by `personal.svg` only.

When there is no profile file for the team (or it is unreadable), the backend
synthesizes a record with `id`, `university.fullName`/`shortName`/`hashTag` and
`team.name` from the contest system, and one entry per roster member carrying
just a `name` plus empty `altNames`/`achievements`.

## The render settings (`{render.json}`)

The same document as `settings.json`, with unset keys omitted:

```json
{
  "contestType": "ICPC",
  "hideHashtag": false,
  "hideSite": false,
  "finals": { "include": true, "includeEmpty": false },
  "fontColor": "#FFFFFF",
  "mainColor": "#4C83C3",
  "logo": "<base64>", "logoExtension": "svg+xml", "background": "<base64>"
}
```

`contestType` is exactly one of `ICPC`, `Team`, `Personal` — an unknown value
makes the whole file fail to load (with a warning in the log) rather than being
silently ignored. `Personal` also tells the backend to build a one-person roster
from the team's short name.

## The fallback chain

For every value the shipped templates use, in order:

1. the substituted token — a colour that is no longer `{mainColor}`/`{fontColor}`,
   or a parsed `{team.json}`/`{render.json}` blob;
2. the corresponding key of `render.json` (colours, logo, background);
3. the built-in defaults compiled into the template, including a full sample team
   so that the file renders as a plausible card when opened directly.

A template detects step 1 by comparing against the literal token text, so a
custom template **must** keep the tokens intact and must behave sensibly when
they are still there. That is not a hypothetical: `{render.json}`, `{fontColor}`
and `{mainColor}` survive every request made without a `settings.json` or for a
team without a colour, and all four survive when the file is opened directly
instead of being served by the backend.
