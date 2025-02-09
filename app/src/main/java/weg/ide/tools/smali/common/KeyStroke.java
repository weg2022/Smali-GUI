package weg.ide.tools.smali.common;

import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_MOVE_END;
import static android.view.KeyEvent.KEYCODE_MOVE_HOME;
import static android.view.KeyEvent.KEYCODE_PAGE_DOWN;
import static android.view.KeyEvent.KEYCODE_PAGE_UP;
import static android.view.KeyEvent.KEYCODE_SPACE;
import static android.view.KeyEvent.KEYCODE_TAB;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.keyCodeToString;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

import android.view.KeyCharacterMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;

public class KeyStroke implements Serializable {

    @Serial
	private static final long serialVersionUID = -1170266913954804590L;
    public static final char CHAR_UNDEFINED = 0xFFFF;
    private final boolean alt;
	private final char ch;
	private final boolean ctrl;
	private final int keyCode;
	private final boolean shift;

	public KeyStroke(int keyCode, char ch, boolean shift, boolean ctrl, boolean alt) {
		this.ch = ch;
		this.keyCode = keyCode;
		this.shift = shift;
		this.ctrl = ctrl;
		this.alt = alt;
	}

	public KeyStroke(int keyCode, boolean shift, boolean ctrl, boolean alt) {
		this.ch = (char) 65535;
		this.keyCode = keyCode;
		this.shift = shift;
		this.ctrl = ctrl;
		this.alt = alt;
	}

	public boolean isChar() {
		return this.ch != CHAR_UNDEFINED;
	}

	public char getChar() {
		return this.ch;
	}

	public int getKeyCode() {
		return this.keyCode;
	}

	public boolean isShift() {
		return this.shift;
	}

	public boolean isCtrl() {
		return this.ctrl;
	}

	public boolean isAlt() {
		return this.alt;
	}

	public boolean matches(@Nullable KeyStroke stroke) {
		if (stroke == null) return false;
		if (this.alt == stroke.alt && this.ctrl == stroke.ctrl && this.shift == stroke.shift) {
			if (this.keyCode == -1 || this.keyCode != stroke.keyCode) {
				return this.ch != CHAR_UNDEFINED && this.ch == stroke.ch;
			}
			return true;
		}
		return false;
	}

	@NonNull
	public String toString() {
		String str = this.shift ? "Shift+" : "";
		if (this.ctrl) {
			str = str + "Ctrl+";
		}
		if (this.alt) {
			str = str + "Alt+";
		}
		return str + getDisplayLabel();
	}

	@NonNull
	private String getDisplayLabel() {
		switch (this.keyCode) {
			case -1 -> {
				return String.valueOf(Character.toUpperCase(this.ch));
			}
			case KEYCODE_DPAD_UP -> {
				return "Up";
			}
			case KEYCODE_DPAD_DOWN -> {
				return "Down";
			}
			case KEYCODE_DPAD_LEFT -> {
				return "Left";
			}
			case KEYCODE_DPAD_RIGHT -> {
				return "Right";
			}
			case KEYCODE_VOLUME_UP -> {
				return "VolUp";
			}
			case KEYCODE_VOLUME_DOWN -> {
				return "VolDown";
			}
			case KEYCODE_TAB -> {
				return "Tab";
			}
			case KEYCODE_SPACE -> {
				return "Space";
			}
			case KEYCODE_ENTER -> {
				return "Enter";
			}
			case KEYCODE_DEL -> {
				return "Backspace";
			}
			case KEYCODE_PAGE_UP -> {
				return "PgUp";
			}
			case KEYCODE_PAGE_DOWN -> {
				return "PgDown";
			}
			case KEYCODE_MOVE_HOME -> {
				return "Home";
			}
			case KEYCODE_MOVE_END -> {
				return "End";
			}
			case KEYCODE_VOLUME_MUTE -> {
				return "VolMute";
			}
			default -> {
				String displayLabel = (String.valueOf(KeyCharacterMap.load(0).getDisplayLabel(this.keyCode))).trim();
				if (displayLabel.length() == 0) {
					String name = keyCodeToString(this.keyCode).toLowerCase();
					if (name.startsWith("keycode_"))
						name = name.substring("keycode_".length());
					
					String finalName = name.replace("_", " ");
					return finalName.substring(0, 1).toUpperCase() + finalName.substring(1);
				}
				return displayLabel;
			}
		}
	}

	@NonNull
	public String store() {
		return this.keyCode + "," + ((int) this.ch) + "," + this.shift + "," + this.ctrl + "," + this.alt;
	}

	@Nullable
	public static KeyStroke load(@NonNull String value) {
		String[] split = value.split(",");
		if (split.length != 5) {
			return null;
		}
		return new KeyStroke(
				parseInt(split[0]),
				(char) parseInt(split[1]),
				parseBoolean(split[2]),
				parseBoolean(split[3]),
				parseBoolean(split[4]));
	}
}
