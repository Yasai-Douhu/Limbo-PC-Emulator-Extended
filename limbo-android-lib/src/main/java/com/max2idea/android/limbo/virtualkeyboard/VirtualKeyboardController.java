package com.max2idea.android.limbo.virtualkeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.limbo.emu.lib.R;
import com.max2idea.android.limbo.main.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Windows準拠 仮想ビルトインキーボード & マウスクイックバー コントローラ
 * - 指の届きやすいマウスクイックバー（L-Click, Wheel, R-Click）
 * - L-Click / R-Click の長押しによるホールド状態（ドラッグ対応）
 * - マウスホイール機能（UP/DOWNボタン + スワイプホイール）
 * - 修飾キー（Shift, Ctrl, Alt, Win）のトグルホールド（ラッチ）
 * - Fnキーによるメイン/サブレイアウト（F1-F12、テンキー、ショートカット）切替
 */
public class VirtualKeyboardController {

    public interface Listener {
        // キー押下・解放イベント
        void onVirtualKeyDown(int keyCode);
        void onVirtualKeyUp(int keyCode);

        // マウスボタン押下・解放イベント (button: Config.SDL_MOUSE_LEFT / RIGHT)
        void onVirtualMouseDown(int button);
        void onVirtualMouseUp(int button);

        // マウスホイールスクロールイベント (deltaY: +1=上, -1=下)
        void onVirtualMouseScroll(int deltaY);
    }

    private final View rootView;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // マウス長押し判定時間（ミリ秒）
    private static final int MOUSE_HOLD_DELAY_MS = 400;

    // マウス左・右クリックのホールド状態管理
    private boolean isLeftMouseLatched = false;
    private boolean isRightMouseLatched = false;
    private boolean leftLongPressTriggered = false;
    private boolean rightLongPressTriggered = false;

    private Runnable leftHoldRunnable;
    private Runnable rightHoldRunnable;

    // 修飾キーのラッチ状態管理 (keyCode -> latched)
    private final Map<Integer, Boolean> latchedKeys = new HashMap<>();

    // UIコンポーネント
    private Button btnMouseLeft;
    private Button btnMouseRight;
    private Button btnWheelUp;
    private Button btnWheelDown;
    private TextView txtWheelCenter;
    private Button btnMinimize;
    private View keyboardBody;
    private View layoutMain;
    private View layoutSub;

    // ホイールスワイプ判定用
    private float lastTouchY = 0f;
    private static final float SWIPE_THRESHOLD = 24f; // スクロール1目盛りに必要なピクセル移動量

    public VirtualKeyboardController(View rootView, Listener listener) {
        this.rootView = rootView;
        this.listener = listener;
        initViews();
    }

    private void initViews() {
        btnMouseLeft = rootView.findViewById(R.id.vk_mouse_left);
        btnMouseRight = rootView.findViewById(R.id.vk_mouse_right);
        btnWheelUp = rootView.findViewById(R.id.vk_wheel_up);
        btnWheelDown = rootView.findViewById(R.id.vk_wheel_down);
        txtWheelCenter = rootView.findViewById(R.id.vk_wheel_center);
        btnMinimize = rootView.findViewById(R.id.vk_btn_minimize);
        keyboardBody = rootView.findViewById(R.id.vk_keyboard_body);
        layoutMain = rootView.findViewById(R.id.vk_layout_main);
        layoutSub = rootView.findViewById(R.id.vk_layout_sub);

        setupMouseButtons();
        setupMouseWheel();
        setupMinimizeButton();
        setupFnSwitch();
        setupMainKeys();
        setupSubKeys();
    }

    /**
     * マウスクリックボタン（左・右）の設定
     * タップで通常クリック、長押し（400ms以上）でホールド（ラッチ）状態へ移行。
     * ホールド中はボタンがハイライトされ、再タップでホールド解除。
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupMouseButtons() {
        // --- 左クリックボタン ---
        btnMouseLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isLeftMouseLatched) {
                            // 既にホールド中の場合はタップで解除
                            releaseLeftMouseHold();
                            return true;
                        }
                        leftLongPressTriggered = false;
                        // 押下ハイライト表示
                        btnMouseLeft.setPressed(true);
                        // 左クリック押下イベント送信
                        if (listener != null) {
                            listener.onVirtualMouseDown(Config.SDL_MOUSE_LEFT);
                        }
                        // 長押しタイマー開始
                        leftHoldRunnable = new Runnable() {
                            @Override
                            public void run() {
                                leftLongPressTriggered = true;
                                isLeftMouseLatched = true;
                                btnMouseLeft.setPressed(false);
                                btnMouseLeft.setBackgroundResource(R.drawable.vk_mouse_btn_latched);
                                btnMouseLeft.setText("L-HOLD");
                                try {
                                    btnMouseLeft.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                                } catch (Exception ignored) {}
                            }
                        };
                        handler.postDelayed(leftHoldRunnable, MOUSE_HOLD_DELAY_MS);
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(leftHoldRunnable);
                        btnMouseLeft.setPressed(false);
                        if (leftLongPressTriggered) {
                            // 長押しホールド状態に移行した場合は、指を離してもDOWN状態を維持（ドラッグ可能）
                            return true;
                        }
                        // 通常の短押しクリック完了（UPイベント送信）
                        if (listener != null) {
                            listener.onVirtualMouseUp(Config.SDL_MOUSE_LEFT);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(leftHoldRunnable);
                        btnMouseLeft.setPressed(false);
                        if (!isLeftMouseLatched && listener != null) {
                            listener.onVirtualMouseUp(Config.SDL_MOUSE_LEFT);
                        }
                        return true;
                }
                return false;
            }
        });

        // --- 右クリックボタン ---
        btnMouseRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isRightMouseLatched) {
                            // 既にホールド中の場合はタップで解除
                            releaseRightMouseHold();
                            return true;
                        }
                        rightLongPressTriggered = false;
                        // 押下ハイライト表示
                        btnMouseRight.setPressed(true);
                        // 右クリック押下イベント送信
                        if (listener != null) {
                            listener.onVirtualMouseDown(Config.SDL_MOUSE_RIGHT);
                        }
                        // 長押しタイマー開始
                        rightHoldRunnable = new Runnable() {
                            @Override
                            public void run() {
                                rightLongPressTriggered = true;
                                isRightMouseLatched = true;
                                btnMouseRight.setPressed(false);
                                btnMouseRight.setBackgroundResource(R.drawable.vk_mouse_btn_latched);
                                btnMouseRight.setText("R-HOLD");
                                try {
                                    btnMouseRight.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                                } catch (Exception ignored) {}
                            }
                        };
                        handler.postDelayed(rightHoldRunnable, MOUSE_HOLD_DELAY_MS);
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(rightHoldRunnable);
                        btnMouseRight.setPressed(false);
                        if (rightLongPressTriggered) {
                            // 長押しホールド状態に移行した場合はDOWN維持
                            return true;
                        }
                        // 通常の短押しクリック完了（UPイベント送信）
                        if (listener != null) {
                            listener.onVirtualMouseUp(Config.SDL_MOUSE_RIGHT);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(rightHoldRunnable);
                        btnMouseRight.setPressed(false);
                        if (!isRightMouseLatched && listener != null) {
                            listener.onVirtualMouseUp(Config.SDL_MOUSE_RIGHT);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /** 左クリックのホールド解除 */
    public void releaseLeftMouseHold() {
        isLeftMouseLatched = false;
        leftLongPressTriggered = false;
        btnMouseLeft.setBackgroundResource(R.drawable.vk_mouse_btn_bg);
        btnMouseLeft.setText("L-Click");
        if (listener != null) {
            listener.onVirtualMouseUp(Config.SDL_MOUSE_LEFT);
        }
    }

    /** 右クリックのホールド解除 */
    public void releaseRightMouseHold() {
        isRightMouseLatched = false;
        rightLongPressTriggered = false;
        btnMouseRight.setBackgroundResource(R.drawable.vk_mouse_btn_bg);
        btnMouseRight.setText("R-Click");
        if (listener != null) {
            listener.onVirtualMouseUp(Config.SDL_MOUSE_RIGHT);
        }
    }

    /**
     * マウスボタン（左または右）がホールド（ラッチ）状態かどうかを取得
     */
    public boolean isMouseLatched() {
        return isLeftMouseLatched || isRightMouseLatched;
    }

    /**
     * マウスホイールの設定 (UP/DOWNボタンおよび中央スワイプ領域)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupMouseWheel() {
        // ホイール上スクロールボタン
        btnWheelUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onVirtualMouseScroll(1);
                }
            }
        });

        // ホイール下スクロールボタン
        btnWheelDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onVirtualMouseScroll(-1);
                }
            }
        });

        // 中央スワイプ領域による直感的なホイール操作
        txtWheelCenter.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchY = event.getY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float diffY = lastTouchY - event.getY();
                        if (Math.abs(diffY) >= SWIPE_THRESHOLD) {
                            int steps = (int) (diffY / SWIPE_THRESHOLD);
                            if (listener != null && steps != 0) {
                                // 上にスワイプした場合は上スクロール(+1)、下にスワイプした場合は下スクロール(-1)
                                listener.onVirtualMouseScroll(steps > 0 ? 1 : -1);
                            }
                            lastTouchY = event.getY();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /** キーボード本体の最小化/展開ボタン */
    private void setupMinimizeButton() {
        btnMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (keyboardBody.getVisibility() == View.VISIBLE) {
                    keyboardBody.setVisibility(View.GONE);
                    btnMinimize.setText("▲");
                } else {
                    keyboardBody.setVisibility(View.VISIBLE);
                    btnMinimize.setText("▼");
                }
            }
        });
    }

    /** Fn切替ボタン */
    private void setupFnSwitch() {
        Button btnFn = rootView.findViewById(R.id.vk_key_fn);
        Button btnFnBack = rootView.findViewById(R.id.vk_key_fn_back);

        View.OnClickListener toggleFn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutMain.getVisibility() == View.VISIBLE) {
                    layoutMain.setVisibility(View.GONE);
                    layoutSub.setVisibility(View.VISIBLE);
                } else {
                    layoutSub.setVisibility(View.GONE);
                    layoutMain.setVisibility(View.VISIBLE);
                }
            }
        };

        if (btnFn != null) btnFn.setOnClickListener(toggleFn);
        if (btnFnBack != null) btnFnBack.setOnClickListener(toggleFn);
    }

    /** メインQWERTYキーボード面のバインド */
    private void setupMainKeys() {
        // 行1
        bindNormalKey(R.id.vk_key_esc, KeyEvent.KEYCODE_ESCAPE);
        bindNormalKey(R.id.vk_key_tab, KeyEvent.KEYCODE_TAB);
        bindNormalKey(R.id.vk_key_grave, KeyEvent.KEYCODE_GRAVE);
        bindNormalKey(R.id.vk_key_1, KeyEvent.KEYCODE_1);
        bindNormalKey(R.id.vk_key_2, KeyEvent.KEYCODE_2);
        bindNormalKey(R.id.vk_key_3, KeyEvent.KEYCODE_3);
        bindNormalKey(R.id.vk_key_4, KeyEvent.KEYCODE_4);
        bindNormalKey(R.id.vk_key_5, KeyEvent.KEYCODE_5);
        bindNormalKey(R.id.vk_key_6, KeyEvent.KEYCODE_6);
        bindNormalKey(R.id.vk_key_7, KeyEvent.KEYCODE_7);
        bindNormalKey(R.id.vk_key_8, KeyEvent.KEYCODE_8);
        bindNormalKey(R.id.vk_key_9, KeyEvent.KEYCODE_9);
        bindNormalKey(R.id.vk_key_0, KeyEvent.KEYCODE_0);
        bindNormalKey(R.id.vk_key_minus, KeyEvent.KEYCODE_MINUS);
        bindNormalKey(R.id.vk_key_equals, KeyEvent.KEYCODE_EQUALS);
        bindNormalKey(R.id.vk_key_backspace, KeyEvent.KEYCODE_DEL);

        // 行2
        bindNormalKey(R.id.vk_key_q, KeyEvent.KEYCODE_Q);
        bindNormalKey(R.id.vk_key_w, KeyEvent.KEYCODE_W);
        bindNormalKey(R.id.vk_key_e, KeyEvent.KEYCODE_E);
        bindNormalKey(R.id.vk_key_r, KeyEvent.KEYCODE_R);
        bindNormalKey(R.id.vk_key_t, KeyEvent.KEYCODE_T);
        bindNormalKey(R.id.vk_key_y, KeyEvent.KEYCODE_Y);
        bindNormalKey(R.id.vk_key_u, KeyEvent.KEYCODE_U);
        bindNormalKey(R.id.vk_key_i, KeyEvent.KEYCODE_I);
        bindNormalKey(R.id.vk_key_o, KeyEvent.KEYCODE_O);
        bindNormalKey(R.id.vk_key_p, KeyEvent.KEYCODE_P);
        bindNormalKey(R.id.vk_key_left_bracket, KeyEvent.KEYCODE_LEFT_BRACKET);
        bindNormalKey(R.id.vk_key_right_bracket, KeyEvent.KEYCODE_RIGHT_BRACKET);
        bindNormalKey(R.id.vk_key_backslash, KeyEvent.KEYCODE_BACKSLASH);
        bindNormalKey(R.id.vk_key_delete, KeyEvent.KEYCODE_FORWARD_DEL);

        // 行3
        bindModifierKey(R.id.vk_key_caps, KeyEvent.KEYCODE_CAPS_LOCK);
        bindNormalKey(R.id.vk_key_a, KeyEvent.KEYCODE_A);
        bindNormalKey(R.id.vk_key_s, KeyEvent.KEYCODE_S);
        bindNormalKey(R.id.vk_key_d, KeyEvent.KEYCODE_D);
        bindNormalKey(R.id.vk_key_f, KeyEvent.KEYCODE_F);
        bindNormalKey(R.id.vk_key_g, KeyEvent.KEYCODE_G);
        bindNormalKey(R.id.vk_key_h, KeyEvent.KEYCODE_H);
        bindNormalKey(R.id.vk_key_j, KeyEvent.KEYCODE_J);
        bindNormalKey(R.id.vk_key_k, KeyEvent.KEYCODE_K);
        bindNormalKey(R.id.vk_key_l, KeyEvent.KEYCODE_L);
        bindNormalKey(R.id.vk_key_semicolon, KeyEvent.KEYCODE_SEMICOLON);
        bindNormalKey(R.id.vk_key_apostrophe, KeyEvent.KEYCODE_APOSTROPHE);
        bindNormalKey(R.id.vk_key_enter, KeyEvent.KEYCODE_ENTER);

        // 行4
        bindModifierKey(R.id.vk_key_shift_left, KeyEvent.KEYCODE_SHIFT_LEFT);
        bindNormalKey(R.id.vk_key_z, KeyEvent.KEYCODE_Z);
        bindNormalKey(R.id.vk_key_x, KeyEvent.KEYCODE_X);
        bindNormalKey(R.id.vk_key_c, KeyEvent.KEYCODE_C);
        bindNormalKey(R.id.vk_key_v, KeyEvent.KEYCODE_V);
        bindNormalKey(R.id.vk_key_b, KeyEvent.KEYCODE_B);
        bindNormalKey(R.id.vk_key_n, KeyEvent.KEYCODE_N);
        bindNormalKey(R.id.vk_key_m, KeyEvent.KEYCODE_M);
        bindNormalKey(R.id.vk_key_comma, KeyEvent.KEYCODE_COMMA);
        bindNormalKey(R.id.vk_key_period, KeyEvent.KEYCODE_PERIOD);
        bindNormalKey(R.id.vk_key_slash, KeyEvent.KEYCODE_SLASH);
        bindModifierKey(R.id.vk_key_shift_right, KeyEvent.KEYCODE_SHIFT_RIGHT);

        // 行5
        bindModifierKey(R.id.vk_key_ctrl, KeyEvent.KEYCODE_CTRL_LEFT);
        bindModifierKey(R.id.vk_key_win, KeyEvent.KEYCODE_WINDOW);
        bindModifierKey(R.id.vk_key_alt, KeyEvent.KEYCODE_ALT_LEFT);
        bindNormalKey(R.id.vk_key_space, KeyEvent.KEYCODE_SPACE);
        bindModifierKey(R.id.vk_key_alt_gr, KeyEvent.KEYCODE_ALT_RIGHT);
        bindNormalKey(R.id.vk_key_left, KeyEvent.KEYCODE_DPAD_LEFT);
        bindNormalKey(R.id.vk_key_up, KeyEvent.KEYCODE_DPAD_UP);
        bindNormalKey(R.id.vk_key_down, KeyEvent.KEYCODE_DPAD_DOWN);
        bindNormalKey(R.id.vk_key_right, KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    /** サブキーボード面（ファンクション、テンキー、ショートカット）のバインド */
    private void setupSubKeys() {
        // F1 - F12
        bindNormalKey(R.id.vk_key_f1, KeyEvent.KEYCODE_F1);
        bindNormalKey(R.id.vk_key_f2, KeyEvent.KEYCODE_F2);
        bindNormalKey(R.id.vk_key_f3, KeyEvent.KEYCODE_F3);
        bindNormalKey(R.id.vk_key_f4, KeyEvent.KEYCODE_F4);
        bindNormalKey(R.id.vk_key_f5, KeyEvent.KEYCODE_F5);
        bindNormalKey(R.id.vk_key_f6, KeyEvent.KEYCODE_F6);
        bindNormalKey(R.id.vk_key_f7, KeyEvent.KEYCODE_F7);
        bindNormalKey(R.id.vk_key_f8, KeyEvent.KEYCODE_F8);
        bindNormalKey(R.id.vk_key_f9, KeyEvent.KEYCODE_F9);
        bindNormalKey(R.id.vk_key_f10, KeyEvent.KEYCODE_F10);
        bindNormalKey(R.id.vk_key_f11, KeyEvent.KEYCODE_F11);
        bindNormalKey(R.id.vk_key_f12, KeyEvent.KEYCODE_F12);

        // 特殊ナビゲーション
        bindNormalKey(R.id.vk_key_prtsc, KeyEvent.KEYCODE_SYSRQ);
        bindNormalKey(R.id.vk_key_sclk, KeyEvent.KEYCODE_SCROLL_LOCK);
        bindNormalKey(R.id.vk_key_pause, KeyEvent.KEYCODE_BREAK);
        bindNormalKey(R.id.vk_key_insert, KeyEvent.KEYCODE_INSERT);
        bindNormalKey(R.id.vk_key_home, KeyEvent.KEYCODE_MOVE_HOME);
        bindNormalKey(R.id.vk_key_end, KeyEvent.KEYCODE_MOVE_END);
        bindNormalKey(R.id.vk_key_pgup, KeyEvent.KEYCODE_PAGE_UP);
        bindNormalKey(R.id.vk_key_pgdn, KeyEvent.KEYCODE_PAGE_DOWN);

        // テンキー
        bindNormalKey(R.id.vk_num_lock, KeyEvent.KEYCODE_NUM_LOCK);
        bindNormalKey(R.id.vk_num_slash, KeyEvent.KEYCODE_NUMPAD_DIVIDE);
        bindNormalKey(R.id.vk_num_star, KeyEvent.KEYCODE_NUMPAD_MULTIPLY);
        bindNormalKey(R.id.vk_num_minus, KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
        bindNormalKey(R.id.vk_num_7, KeyEvent.KEYCODE_NUMPAD_7);
        bindNormalKey(R.id.vk_num_8, KeyEvent.KEYCODE_NUMPAD_8);
        bindNormalKey(R.id.vk_num_9, KeyEvent.KEYCODE_NUMPAD_9);
        bindNormalKey(R.id.vk_num_plus, KeyEvent.KEYCODE_NUMPAD_ADD);
        bindNormalKey(R.id.vk_num_4, KeyEvent.KEYCODE_NUMPAD_4);
        bindNormalKey(R.id.vk_num_5, KeyEvent.KEYCODE_NUMPAD_5);
        bindNormalKey(R.id.vk_num_6, KeyEvent.KEYCODE_NUMPAD_6);
        bindNormalKey(R.id.vk_num_left_paren, KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);
        bindNormalKey(R.id.vk_num_1, KeyEvent.KEYCODE_NUMPAD_1);
        bindNormalKey(R.id.vk_num_2, KeyEvent.KEYCODE_NUMPAD_2);
        bindNormalKey(R.id.vk_num_3, KeyEvent.KEYCODE_NUMPAD_3);
        bindNormalKey(R.id.vk_num_right_paren, KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);
        bindNormalKey(R.id.vk_num_0, KeyEvent.KEYCODE_NUMPAD_0);
        bindNormalKey(R.id.vk_num_dot, KeyEvent.KEYCODE_NUMPAD_DOT);
        bindNormalKey(R.id.vk_num_enter, KeyEvent.KEYCODE_NUMPAD_ENTER);

        // サブ画面用のナビゲーションキー
        bindNormalKey(R.id.vk_key_sub_esc, KeyEvent.KEYCODE_ESCAPE);
        bindNormalKey(R.id.vk_key_sub_tab, KeyEvent.KEYCODE_TAB);
        bindNormalKey(R.id.vk_key_sub_del, KeyEvent.KEYCODE_FORWARD_DEL);
    }

    /**
     * 通常キーのバインド (押下でDOWN、離してUP)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void bindNormalKey(int viewId, final int keyCode) {
        final View v = rootView.findViewById(viewId);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (listener != null) {
                            listener.onVirtualKeyDown(keyCode);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        if (listener != null) {
                            listener.onVirtualKeyUp(keyCode);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * 修飾キーのバインド（Shift, Ctrl, Alt, Win, CapsLock）
     * - タップ（短押し）: 通常のキーと同じく単発送信（KeyDown -> KeyUp）
     * - 長押し（400ms以上）: ホールド状態（ラッチ）となり青く点灯。再度タップでホールド解除。
     */
    @SuppressLint("ClickableViewAccessibility")
    private void bindModifierKey(int viewId, final int keyCode) {
        final Button v = rootView.findViewById(viewId);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isLongPress = false;
            private boolean wasAlreadyLatched = false;
            private final Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPress = true;
                    latchedKeys.put(keyCode, true);
                    v.setBackgroundResource(R.drawable.vk_key_bg_latched);
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {}
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        isLongPress = false;
                        wasAlreadyLatched = Boolean.TRUE.equals(latchedKeys.get(keyCode));

                        if (wasAlreadyLatched) {
                            // 既にホールド中の場合はUP時に解除
                        } else {
                            // まだホールドされていない場合はKeyDownを送信し、長押しタイマー起動
                            if (listener != null) {
                                listener.onVirtualKeyDown(keyCode);
                            }
                            handler.postDelayed(longPressRunnable, 400);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        handler.removeCallbacks(longPressRunnable);

                        if (wasAlreadyLatched) {
                            // 既にホールド中だったキーをタップした場合はホールド解除
                            latchedKeys.put(keyCode, false);
                            v.setBackgroundResource(R.drawable.vk_key_bg);
                            if (listener != null) {
                                listener.onVirtualKeyUp(keyCode);
                            }
                        } else if (isLongPress) {
                            // 長押しでホールド確定した場合: 指を離してもKeyDown状態を維持
                        } else {
                            // 短押し（タップ）: 通常キーと同じく単発KeyUpを送信して終了（ホールドしない）
                            if (listener != null) {
                                listener.onVirtualKeyUp(keyCode);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        handler.removeCallbacks(longPressRunnable);
                        if (!Boolean.TRUE.equals(latchedKeys.get(keyCode))) {
                            if (listener != null) {
                                listener.onVirtualKeyUp(keyCode);
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * ショートカットキーのバインド (順次KeyDown -> 順次KeyUp)
     */
    private void bindShortcutKey(int viewId, final int[] comboKeyCodes) {
        View v = rootView.findViewById(viewId);
        if (v == null) return;

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener == null) return;
                // 順番にKeyDown
                for (int code : comboKeyCodes) {
                    listener.onVirtualKeyDown(code);
                }
                // 50ms後に順番にKeyUp
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = comboKeyCodes.length - 1; i >= 0; i--) {
                            listener.onVirtualKeyUp(comboKeyCodes[i]);
                        }
                    }
                }, 50);
            }
        });
    }

    /** 全てのホールド/ラッチ状態をリセット */
    public void resetLatches() {
        if (isLeftMouseLatched) {
            releaseLeftMouseHold();
        }
        if (isRightMouseLatched) {
            releaseRightMouseHold();
        }
        for (Map.Entry<Integer, Boolean> entry : latchedKeys.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                if (listener != null) {
                    listener.onVirtualKeyUp(entry.getKey());
                }
            }
        }
        latchedKeys.clear();
    }
}
