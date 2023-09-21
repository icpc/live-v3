# Typical problems

## Overlay backend is not starting or sends no data

* Check `icpclive.log` file, there are probably some useful exceptions
  * If it has some SSL exceptions
    * Add `"network": { "allowUnsecureConnections": true }` into your settings file, 
      if you are fine with ignoring certificate check
  * If there are some FileNotFound exceptions
    * Check your command line arguments, maybe some pathes are relative to unexpected source
  * If there are some other crashes
    * Sorry, report a bug to us
* Check frontend log by opening `http://localhost:8080/overlay` in browser and
  scrolling down.

## Overlay works, but no updates after some time

* If you are using clics check your clics server has keep-alive enabled
* If you are using codeforces, check if you are not banned by their anti-ddos.
  * Write to Mike if you are :(