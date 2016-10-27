In the specs I mentionned I would use Retrofit, but for my purposes (querying my backend via http) OkHttp made much more sense so I'm using that.

There's a google cloud instance that acts as this app's backend to sync the user's data over multiple devices (and the cloud).
I was looking for a technical challenge so I picked it even though it's 'overkill' for this app.


# Known issues

* The database of translations is really poor: there are few words and they are sometimes poorly translated.
Courtesy of microsoft bing.
* The card flipping animation crashes on (emulated) tablets.
I left it as is on tablet because I'm still somehow hoping it works on a real device.

I already disabled the card flipping animation on landscape layouts.
Unfortunately it was crashing the app due to an OpenGL bug I was not able to fix.
I reduced the animation to its simplest form so next step is to remove it altogether.




