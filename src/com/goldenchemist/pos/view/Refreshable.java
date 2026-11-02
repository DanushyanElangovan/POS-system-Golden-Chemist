package com.goldenchemist.pos.view;

/** Implemented by screens that reload their data every time they are shown. */
public interface Refreshable {
    void onShow();
}
