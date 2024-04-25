/*
    Copyright (c) 2015-2024 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
/*
   Automatically remove auto-favorites for every user and disable favorites
   going forward for affected users.

   Since new users get created on a regular basis during onboarding this script
   could run periodically to clean up newly onboarded users.
 */

import hudson.model.User
import io.jenkins.blueocean.autofavorite.user.FavoritingUserProperty
import hudson.plugins.favorite.user.FavoriteUserProperty

User.all.each { user ->
	if(!user?.getProperty(FavoritingUserProperty)?.autofavoriteEnabled) {
		return
	}
	user.getProperty(FavoritingUserProperty).with { autoFavorite ->
		autoFavorite.autofavoriteEnabled = false
	}
	user.getProperty(FavoriteUserProperty).with { favorites ->
		favorites.allFavorites.each { String cursed ->
			favorites.removeFavorite(cursed)
		}
	}
	println "Disabled auto-favoriting for ${user}."
	println "Removed all favorites from ${user}."
	user.save()
}
null
