## TL;DR

* Examples [contests](https://github.com/icpc/live-v3/tree/main/config/_examples) for all supported contest systems
  * [CLICS](https://github.com/icpc/live-v3/tree/main/config/icpc-north-america/mcpc-2024) 
  * [PCMS](https://github.com/icpc/live-v3/tree/main/config/icpc-northern-eurasia/nef-2024-2025)
  * [Codeforces](https://github.com/icpc/live-v3/tree/main/config/vkoshp/2024-junior)
* See the [full archive](https://github.com/icpc/live-v3/tree/main/config) for more examples

## Config file

Main config should be stored within config directory in `settings.json` or `settings.json5` file.

Settings should specify the contest system used and parameters of the contest.
Also, it sometimes contains some abilities to modify received data.

You can use [json schema file](https://github.com/icpc/live-v3/tree/main/schemas/settings.schema.json) in your text editor to get help with writing config. 

[Here](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.settings/-c-d-s-settings/index.html) 
is the full list of supported properties for all systems.

Typical config should look like that

```
// settings.json file in your config directory
{
  "type": "cf",
  "apiKey": "$creds.cf_api_key",
  "apiSecret": "$creds.cf_api_secret",
  "contestId": 1600,
  "emulation": { "speed": 10, "startTime": "2023-08-24 22:43"}, // remove this if you don't need emulation
}
// creds.json file somewhere else passed with --creds option
{
  "cf_api_key": "YOUR CODEFORCES API KEY",
  "cf_api_secret": "YOUR CODEFORCES API SECRET",
}
```

Other contest systems have different arguments than apiKey/apiSecret/contestId but the main idea is the same.

## Providing authorization credentials

Whenever you need to put some url in config, you can put it in several ways:

* As just a string. Then it would be interpreted as url, if it starts with `http://` or `https://`,
  otherwise, it would be interpreted as local file, which is useful for testing.
* As an object with url, login, and password
* As an object with url and full [auth](https://icpc.io/live-v3/cds/cds/core/org.icpclive.cds.settings/-auth/index.html) object.

A separate creds file is needed to avoid accidental publishing of your keys. 
If you don't plan to publish a config file, you can just put the key into it instead of a separate file.

For example:

```
// settings.json with just an url
{
  "type": "pcms",
  "source": "https://nerc.itmo.ru/archive/2024/standings.xml"
}
// settings.json with url, requiring password stored in creds file
{
  "type": "clics",
  "feeds": [
    {
      "source": {
        "url": "https://swerc.vps.tecnico.ulisboa.pt/domjudge/api",
        "login": "lidia",
        "password": "$creds.password"
      },
      "contestId": "4",
      "feedVersion": "2023_06"
    }
  ]
}
```

