/*
Copyright (C) Max Kastanas 2012
Modifications for SDL3 / sdl2-compat portability 2026

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
#include <stdbool.h>
#include <string.h>
#include <jni.h>
#include <SDL.h>
#include "SDL_limbomouse.h"

#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_MOVE 2
#define ACTION_HOVER_MOVE 7
#define ACTION_SCROLL 8
/* Java側 Config.java の定数と一致させる: LEFT=1, MIDDLE=2, RIGHT=3 */
#define BUTTON_LEFT   1
#define BUTTON_MIDDLE 2
#define BUTTON_RIGHT  3

static int x_min = 0, x_max = 0, y_min = 0, y_max = 0;
static bool checkBounds = false;
/* 現在押下中のマウスボタン状態（QEMU側のドラッグ・ホールド判定に必須） */
static Uint32 current_button_state = 0;

// SDL公開イベントAPIを用いてマウスイベントを送信
JNIEXPORT void JNICALL Java_com_max2idea_android_limbo_jni_VMExecutor_nativeMouseEvent(
        JNIEnv* env, jobject thiz,
        int button, int action, int relative, int x, int y) {

    SDL_Window *win = SDL_GetGrabbedWindow();
    if (!win) {
        win = SDL_GetMouseFocus();
    }
    if (!win) {
        win = SDL_GetKeyboardFocus();
    }
    if (!win) {
        win = SDL_GetWindowFromID(1);
    }
    Uint32 windowID = win ? SDL_GetWindowID(win) : 1;

    /* 仮想ボタンクリック（x=0, y=0）時は既存の相対モード状態を勝手に切り替えない */
    if (action == ACTION_MOVE || action == ACTION_HOVER_MOVE || (x != 0 || y != 0)) {
        SDL_bool relativeMouseMode = relative ? SDL_TRUE : SDL_FALSE;
        if (SDL_GetRelativeMouseMode() != relativeMouseMode) {
            SDL_SetRelativeMouseMode(relativeMouseMode);
        }
    }

    if (checkBounds) {
        if (!relative) {
            if (x < x_min) x = x_min;
            if (x > x_max) x = x_max;
            if (y < y_min) y = y_min;
            if (y > y_max) y = y_max;
        }
    }

    Uint8 sdl_button = SDL_BUTTON_LEFT;
    if (button == BUTTON_LEFT)        sdl_button = SDL_BUTTON_LEFT;
    else if (button == BUTTON_MIDDLE) sdl_button = SDL_BUTTON_MIDDLE;
    else if (button == BUTTON_RIGHT)  sdl_button = SDL_BUTTON_RIGHT;

    SDL_Event ev;
    memset(&ev, 0, sizeof(ev));

    switch (action) {
        case ACTION_DOWN:
        case ACTION_UP:
            if (action == ACTION_DOWN) {
                current_button_state |= SDL_BUTTON(sdl_button);
            } else {
                current_button_state &= ~SDL_BUTTON(sdl_button);
            }

            /* 絶対座標指定かつ有効な座標の場合のみカーソル位置を合わせる */
            if (!relative && (x != 0 || y != 0)) {
                SDL_Event motion_ev;
                memset(&motion_ev, 0, sizeof(motion_ev));
                motion_ev.type = SDL_MOUSEMOTION;
                motion_ev.motion.windowID = windowID;
                motion_ev.motion.which = SDL_TOUCH_MOUSEID;
                motion_ev.motion.state = current_button_state;
                motion_ev.motion.x = x;
                motion_ev.motion.y = y;
                SDL_PushEvent(&motion_ev);
            }
            ev.type = (action == ACTION_DOWN) ? SDL_MOUSEBUTTONDOWN : SDL_MOUSEBUTTONUP;
            ev.button.windowID = windowID;
            ev.button.which = SDL_TOUCH_MOUSEID;
            ev.button.button = sdl_button;
            ev.button.state = (action == ACTION_DOWN) ? SDL_PRESSED : SDL_RELEASED;
            ev.button.clicks = 1;
            ev.button.x = x;
            ev.button.y = y;
            SDL_PushEvent(&ev);
            break;

        case ACTION_MOVE:
        case ACTION_HOVER_MOVE:
            ev.type = SDL_MOUSEMOTION;
            ev.motion.windowID = windowID;
            ev.motion.which = SDL_TOUCH_MOUSEID;
            ev.motion.state = current_button_state; /* ドラッグ中のボタン状態を伝達 */
            if (relative) {
                ev.motion.xrel = x;
                ev.motion.yrel = y;
            } else {
                ev.motion.x = x;
                ev.motion.y = y;
            }
            SDL_PushEvent(&ev);
            break;

        case ACTION_SCROLL:
            ev.type = SDL_MOUSEWHEEL;
            ev.wheel.windowID = windowID;
            ev.wheel.which = SDL_TOUCH_MOUSEID;
            ev.wheel.x = x;
            ev.wheel.y = y;
            ev.wheel.direction = SDL_MOUSEWHEEL_NORMAL;
            SDL_PushEvent(&ev);
            break;

        default:
            break;
    }
}

JNIEXPORT void JNICALL Java_com_max2idea_android_limbo_jni_VMExecutor_nativeMouseBounds(
        JNIEnv* env, jobject thiz, int xmin, int xmax, int ymin, int ymax) {
    checkBounds = true;
    x_min = xmin + 1;
    x_max = xmax - 1;
    y_min = ymin + 1;
    y_max = ymax - 1;
}

