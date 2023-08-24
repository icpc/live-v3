# live-v3

Welcome to the ICPC Live Source Code Repository.

# Run release version

* Download release from https://github.com/icpc/live-v3/releases
* Create contest config files
    * [Example for CLICS](https://github.com/icpc/live-v3/tree/main/config/icpc-rmc/2021)
    * [Example for PCMS](https://github.com/icpc/live-v3/tree/main/config/icpc-nef/2021-2022/main)
    * [Example for Codeforces](https://github.com/icpc/live-v3/tree/main/config/vkoshp/2022-junior)
    * [Example for Yandex.Contest](https://github.com/icpc/live-v3/tree/main/config/_examples/_yandex/)
    * [Example for Ejudge](https://github.com/icpc/live-v3/tree/main/config/lscpc/2022/)
    * [See the full archive for more examples](https://github.com/icpc/live-v3/tree/main/config)
    * [How to fine tune imported data](https://github.com/icpc/live-v3/blob/main/docs/advanced.json.md)
    * [How to get the current state of imported data](http://localhost:8080/api/admin/advancedJsonPreview?fields=all)



* Run `java -jar /path/to/jar/file -c /path/to/config/directory`

  Check for more options by running `java -jar /path/to/jar/file` without arguments. Here is the couple most useful
  * ```--creds creds.json``` -- The path to the credential file. It can be used to avoid storing credentials in the main config file. 
  * ```--no-auth``` -- Disable auth in admin interface. It's useful if you are running the overlayer on localhost.
  * ```-p 8080``` -- 8080 is default port to listen, but it can be changed.


* Add source to OBS
    * +Source
    * Browser
    * URL http://localhost:8080/overlay?noStatus
    * W H 1920x1080
    * OBS Custom css:

```css
#root > div {
    background: unset;
}
```

* Use http://localhost:8080/admin in your browser to control overlay
* Check general broadcast production [schema](https://docs.google.com/document/d/1JcOhmkvbRtG3MLLYUpzVBMqiQOoNpamOz-MvppCgcYk) for other details of typical setup

Also, check [emulation mode](https://github.com/icpc/live-v3/blob/main/docs/emulation.md) part of development doc for testing.

# Authorisation

For now http basic auth is used. If you try to login with
user, which does not exist, it will be automatically created.
First created receives admin rights. Others only receive them when
someone with admin rights confirms, it's okay.

Consider, if you are okay with passing your passwords using plain HTTP.

If you don't need auth, you can disable it by `--no-auth` command-line option.

# Run in development mode

[Check separate document](https://github.com/icpc/live-v3/blob/main/docs/development.md)

# Previous versions:

* https://github.com/icpc-live
* https://github.com/Aksenov239/icpc-live-v2

# Other repos:

* [Script](https://github.com/EgorKulikov/acm_profiles) that collects competitive programming historical data for
  analytical information
* [Autoanalyst](https://github.com/icpc-live/autoanalyst)
* OBS Video Scheduler [Plugin](https://github.com/pashkal/obs-video-scheduler) with web interface
* Outdated OBS [plugin](https://github.com/pmavrin/obs-overlays/tree/master/overlaymaster) for shared memory ([dll](https://drive.google.com/file/d/1MvCmhlSpftUFC3N2gj0Lv88-ZV2dtnhP))

For more information, email `live@icpc.global`