
In the specs I mentionned I would use Retrofit, but for my purposes (querying my backend via http) OkHttp made much more sense so I'm using that.


I had to disable the card flipping animation on landscape layouts. Unfortunately it was crashing the app due to an OpenGL bug I was not able to fix.
I did not have the chance to try it on a real device but on my emulators the card flipping animation looks quite sluggish sometimes.




# Known issues

The card flipping animation crashes on (emulated) tablets.
I did not have the chance to try it on a real device, I hope it performs better.

I reduced the animation to its simplest form so next step is to remove it altogether.




