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
#include <jni.h>
#include <SDL.h>
#include "SDL_limboscreen.h"

// SDL公開APIのみを用いてフルスクリーン切替を実行
JNIEXPORT void JNICALL Java_com_max2idea_android_limbo_jni_VMExecutor_nativeFullscreen(
        JNIEnv* env, jobject thiz) {
    SDL_Window *win = SDL_GetGrabbedWindow();
    if (!win) {
        win = SDL_GetMouseFocus();
    }
    if (!win) {
        win = SDL_GetKeyboardFocus();
    }
    if (win) {
        SDL_SetWindowFullscreen(win, SDL_WINDOW_FULLSCREEN_DESKTOP);
    }
}


