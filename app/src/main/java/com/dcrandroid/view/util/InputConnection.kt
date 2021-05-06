/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.view.util

import android.text.Editable
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection

/**
 *
 * @author Carl Gunther
 * There are bugs with the LatinIME keyboard's generation of KEYCODE_DEL events
 * that this class addresses in various ways.
 *
 * This class is intended for use by a view that overrides
 * onCreateInputConnection() and specifies to the invoking IME that it wishes
 * to use the InputType. This should cause key events to be returned
 * to the view.
 *
 */
class InputConnectionAccommodatingLatinIMETypeNullIssues(targetView: View?, fullEditor: Boolean) :
        BaseInputConnection(targetView, fullEditor) {

    // This holds the Editable text buffer that the LatinIME mistakenly *thinks*
    // that it is editing, even though the views that employ this class are
    // completely driven by key events.
    private var myEditable: Editable? = null

    // This method is called by the IME whenever the view that returned an
    // instance of this class to the IME from its onCreateInputConnection()
    // gains focus.
    override fun getEditable(): Editable? {
        // Some versions of the Google Keyboard (LatinIME) were delivered with a
        // bug that causes KEYCODE_DEL to no longer be generated once the number
        // of KEYCODE_DEL taps equals the number of other characters that have
        // been typed.  This bug was reported here as issue 62306.
        //
        // As of this writing (1/7/2014), it is fixed in the AOSP code, but that
        // fix has not yet been released.  Even when it is released, there will
        // be many devices having versions of the Google Keyboard that include the bug
        // in the wild for the indefinite future.  Therefore, a workaround is required.
        //
        // This is a workaround for that bug which just jams a single garbage character
        // into the internal buffer that the keyboard THINKS it is editing even
        // though we have specified TYPE_NULL which *should* cause LatinIME to
        // generate key events regardless of what is in that buffer.  We have other
        // code that attempts to ensure as the user edits that there is always
        // one character remaining.
        //
        // The problem arises because when this unseen buffer becomes empty, the IME
        // thinks that there is nothing left to delete, and therefore stops
        // generating KEYCODE_DEL events, even though the app may still be very
        // interested in receiving them.
        //
        // So, for example, if the user taps in ABCDE and then positions the
        // (app-based) cursor to the left of A and taps the backspace key three
        // times without any evident effect on the letters (because the app's own
        // UI code knows that there are no letters to the left of the
        // app-implemented cursor), and then moves the cursor to the right of the
        // E and hits backspace five times, then, after E and D have been deleted,
        // no more KEYCODE_DEL events will be generated by the IME because the
        // unseen buffer will have become empty from five letter key taps followed
        // by five backspace key taps (as the IME is unaware of the app-based cursor
        // movements performed by the user).
        //
        // In other words, if your app is processing KEYDOWN events itself, and
        // maintaining its own cursor and so on, and not telling the IME anything
        // about the user's cursor position, this buggy processing of the hidden
        // buffer will stop KEYCODE_DEL events when your app actually needs them -
        // in whatever Android releases incorporate this LatinIME bug.
        //
        // By creating this garbage characters in the Editable that is initially
        // returned to the IME here, we make the IME think that it still has
        // something to delete, which causes it to keep generating KEYCODE_DEL
        // events in response to backspace key presses.
        //
        // A specific keyboard version that I tested this on which HAS this
        // problem but does NOT have the "KEYCODE_DEL completely gone" (issue 42904)
        // problem that is addressed by the deleteSurroundingText() override below
        // (the two problems are not both present in a single version) is
        // 2.0.19123.914326a, tested running on a Nexus7 2012 tablet.
        // There may be other versions that have issue 62306.
        //
        // A specific keyboard version that I tested this on which does NOT have
        // this problem but DOES have the "KEYCODE_DEL completely gone" (issue
        // 42904) problem that is addressed by the deleteSurroundingText()
        // override below is 1.0.1800.776638, tested running on the Nexus10
        // tablet.  There may be other versions that also have issue 42904.
        //
        // The bug that this addresses was first introduced as of AOSP commit tag
        // 4.4_r0.9, and the next RELEASED Android version after that was
        // android-4.4_r1, which is the first release of Android 4.4.  So, 4.4 will
        // be the first Android version that would have included, in the original
        // RELEASED version, a Google Keyboard for which this bug was present.
        //
        // Note that this bug was introduced exactly at the point that the OTHER bug
        // (the one that is addressed in deleteSurroundingText(), below) was first
        // FIXED.
        //
        // Despite the fact that the above are the RELEASES associated with the bug,
        // the fact is that any 4.x Android release could have been upgraded by the
        // user to a later version of Google Keyboard than was present when the
        // release was originally installed to the device.  I have checked the
        // www.archive.org snapshots of the Google Keyboard listing page on the Google
        // Play store, and all released updates listed there (which go back to early
        // June of 2013) required Android 4.0 and up, so we can be pretty sure that
        // this bug is not present in any version earlier than 4.0 (ICS), which means
        // that we can limit this fix to API level 14 and up.  And once the LatinIME
        // problem is fixed, we can limit the scope of this workaround to end as of
        // the last release that included the problem, since we can assume that
        // users will not upgrade Google Keyboard to an EARLIER version than was
        // originally included in their Android release.
        //
        // The bug that this addresses was FIXED but NOT RELEASED as of this AOSP
        // commit:
        // https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+
        // /b41bea65502ce7339665859d3c2c81b4a29194e4/java/src/com/android
        // /inputmethod/latin/LatinIME.java
        // so it can be assumed to affect all of KitKat released thus far
        // (up to 4.4.2), and could even affect beyond KitKat, although I fully
        // expect it to be incorporated into the next release *after* API level 19.
        //
        // When it IS released, this method should be changed to limit it to no
        // higher than API level 19 (assuming that the fix is released before API
        // level 20), just in order to limit the scope of this fix, since poking
        // 1024 characters into the Editable object returned here is of course a
        // kluge. But right now the safest thing is just to not have an upper limit
        // on the application of this kluge, since the fix for the problem it
        // addresses has not yet been released (as of 1/7/2014).
        if (myEditable == null) {
            myEditable = EditableAccommodatingLatinIMETypeNullIssues()
        }
        return myEditable
    }

    // This method is called INSTEAD of generating a KEYCODE_DEL event, by
    // versions of Latin IME that have the bug described in Issue 42904.
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        // If targetSdkVersion is set to anything AT or ABOVE API level 16
        // then for the GOOGLE KEYBOARD versions DELIVERED
        // with Android 4.1.x, 4.2.x or 4.3.x, NO KEYCODE_DEL EVENTS WILL BE
        // GENERATED BY THE GOOGLE KEYBOARD (LatinIME) EVEN when TYPE_NULL
        // is being returned as the InputType by your view from its
        // onCreateInputMethod() override, due to a BUG in THOSE VERSIONS.
        //
        // When TYPE_NULL is specified (as this entire class assumes is being done
        // by the views that use it, what WILL be generated INSTEAD of a KEYCODE_DEL
        // is a deleteSurroundingText(1,0) call.  So, by overriding this
        // deleteSurroundingText() method, we can fire the KEYDOWN/KEYUP events
        // ourselves for KEYCODE_DEL.  This provides a workaround for the bug.
        //
        // The specific AOSP RELEASES involved are 4.1.1_r1 (the very first 4.1
        // release) through 4.4_r0.8 (the release just prior to Android 4.4).
        // This means that all of KitKat should not have the bug and will not
        // need this workaround.
        //
        // Although 4.0.x (ICS) did not have this bug, it was possible to install
        // later versions of the keyboard as an app on anything running 4.0 and up,
        // so those versions are also potentially affected.
        //
        // The first version of separately-installable Google Keyboard shown on the
        // Google Play store site by www.archive.org is Version 1.0.1869.683049,
        // on June 6, 2013, and that version (and probably other, later ones)
        // already had this bug.
        //
        // Since this required at least 4.0 to install, I believe that the bug will
        // not be present on devices running versions of Android earlier than 4.0.
        //
        // AND, it should not be present on versions of Android at 4.4 and higher,
        // since users will not "upgrade" to a version of Google Keyboard that
        // is LOWER than the one they got installed with their version of Android
        // in the first place, and the bug will have been fixed as of the 4.4 release.
        //
        // The above scope of the bug is reflected in the test below, which limits
        // the application of the workaround to Android versions between 4.0.x and 4.3.x.
        //
        // UPDATE: A popular third party keyboard was found that exhibits this same issue. It
        // was not fixed at the same time as the Google Play keyboard, and so the bug in that case
        // is still in place beyond API LEVEL 19. So, even though the Google Keyboard fixed this
        // as of level 19, we cannot take out the fix based on that version number. And so I've
        // removed the test for an upper limit on the version; the fix will remain in place ad
        // infinitum.
        return if (beforeLength == 1 && afterLength == 0) {
            // Send Backspace key down and up events to replace the ones omitted
            // by the LatinIME keyboard.
            (super.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    && super.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)))
        } else {
            // Really, I can't see how this would be invoked, given that we're using
            // TYPE_NULL, for non-buggy versions, but in order to limit the impact
            // of this change as much as possible (i.e., to versions at and above 4.0)
            // I am using the original behavior here for non-affected versions.
            super.deleteSurroundingText(beforeLength, afterLength)
        }
    }
}

class EditableAccommodatingLatinIMETypeNullIssues : SpannableStringBuilder("") {

    override fun replace(spannableStringStart: Int, spannableStringEnd: Int,
                         replacementSequence: CharSequence?, replacementStart: Int, replacementEnd: Int): SpannableStringBuilder {
        if (replacementEnd > replacementStart) {
            // In this case, there is something in the replacementSequence that the IME
            // is attempting to replace part of the editable with.
            // We don't really care about whatever might already be in the editable;
            // we only care about making sure that SOMETHING ends up in it,
            // so that the backspace key will continue to work.
            // So, start by zeroing out whatever is there to begin with.
            super.replace(0, length, "", 0, 0)

            // We DO care about preserving the new stuff that is replacing the stuff in the
            // editable, because this stuff might be sent to us as a keydown event.  So, we
            // insert the new stuff (typically, a single character) into the now-empty editable,
            // and return the result to the caller.
            return super.replace(0, 0, replacementSequence, replacementStart, replacementEnd)
        } else if (spannableStringEnd > spannableStringStart) {
            // In this case, there is NOTHING in the replacementSequence, and something is
            // being replaced in the editable.
            // This is characteristic of a DELETION.
            // So, start by zeroing out whatever is being replaced in the editable.
            super.replace(0, length, "", 0, 0)

            // And now, we will place our ONE_UNPROCESSED_CHARACTER into the editable buffer, and return it.
            return super.replace(0, 0, "", 0, 0)
        }

        // In this case, NOTHING is being replaced in the editable.  This code assumes that there
        // is already something there.  This assumption is probably OK because in our
        // InputConnectionAccommodatingLatinIMETypeNullIssues.getEditable() method
        // we PLACE a ONE_UNPROCESSED_CHARACTER into the newly-created buffer.  So if there
        // is nothing replacing the identified part
        // of the editable, and no part of the editable that is being replaced, then we just
        // leave whatever is in the editable ALONE,
        // and we can be confident that there will be SOMETHING there.  This call to super.replace()
        // in that case will be a no-op, except
        // for the value it returns.
        return super.replace(spannableStringStart, spannableStringEnd,
                replacementSequence, replacementStart, replacementEnd)
    }
}