/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.ecmdroid;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.ecmdroid.ECM.Type;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

/**
 * This class allows manipulating the bytes stored in the ECMs EEPROM.
 */
public class EEPROM {

	private static final String TAG = "EEPROM";

	private ECM.Type type;
	private String id;
	private String version;
	private ArrayList<Page> pages;
	private int length = 0;
	private byte[] data;
	private boolean eepromRead;
	private boolean touched;
	private int xsize = 0;

	public EEPROM(String id) {
		this.id = id;
		pages = new ArrayList<Page>();
	}

	public int length() {
		return length;
	}


	public byte[] getBytes() {
		return data;
	}

	public void setBytes(byte[] data) {
		this.data = data;
		this.length = data.length;
	}

	public Collection<Page> getPages() {
		return pages;
	}

	public void addPage(Page page) {
		pages.add(page);
	}

	public String getId() {
		return id;
	}

	public ECM.Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "EEPROM[id: " + id + ", type: " + type + ", version: " + version + ", length: " + length + ", number of pages: " + pages.size() + "]";
	}

	public static EEPROM get(String name, Context ctx) {
		if (name == null) {
			return null;
		}
		if (name.length() > 5) {
			name = name.substring(0, 5);
		}
		DBHelper helper = new DBHelper(ctx);
		EEPROM eeprom = null;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = null;
		try {
			String query = "SELECT xsize, type, page, pages.size as pgsize" +
					" FROM eeprom, pages" +
					" WHERE pages.category = eeprom.category" +
					" AND name = '" + name + "'" +
					" ORDER BY page";
			// Log.d(TAG, query);
			c = db.rawQuery(query, null);
			if (c.getCount() == 0) {
				return null;
			}
			eeprom = new EEPROM(name);
			int pc = 0;
			while (c.moveToNext()) {
				if (eeprom.length == 0) {
					eeprom.length = c.getInt(c.getColumnIndex("xsize"));
					eeprom.xsize = eeprom.length;
					eeprom.type = Type.getType(c.getString(c.getColumnIndex("type")));
					eeprom.data = new byte[eeprom.length];
				}
				int pnr = c.getInt(c.getColumnIndex("page"));
				int sz = c.getInt(c.getColumnIndex("pgsize"));
				Page pg = eeprom.new Page(pnr, sz);
				if (pnr == 0) {
					pg.start = eeprom.length - pg.length;
				} else {
					pg.start = pc;
					pc += pg.length;
				}
				eeprom.pages.add(pg);
			}
		} finally {
			if (c != null) {
				c.close();
			}
			db.close();
		}
		return eeprom;
	}

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
	public static String bytesToHex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		String result = new String(hexChars, StandardCharsets.UTF_8);
		ArrayList<String> split = new ArrayList<>();
		for (int i = 0; i <= result.length() / 2; i++) {
			split.add(result.substring(i * 2, Math.min((i + 1) * 2, result.length())));
		}
		return split.toString();
	}
	public static EEPROM load(Context context, String id, InputStream in) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) > 0) {
			bytes.write(buffer, 0, length);
		}
		bytes.flush();
		in.close();

		byte[] data = bytes.toByteArray();

		EEPROM eeprom = EEPROM.get(id, context);
		if (eeprom == null) {
			throw new FileNotFoundException(context.getString(R.string.unsupported_eeprom, id));
		}
		eeprom.setBytes(data);
		for (Page pg : eeprom.getPages()) {
			pg.touch();
			/*
			if (pg.nr() == 0) {
				// Debug only - Check if this has AFV front?
				if (eeprom.getType() == ECM.Type.DDFI3) {
					Log.w(TAG, "Reading Page " + pg.toString());
					Log.w(TAG, "Writing Page Data" + bytesToHex(pg.getBytes(0,pg.length(), new byte[pg.length()],0)));
					ECM ecm = ECM.getInstance(context);
					ecm.writeEEPromPage(pg, -22, 2);
				}
			}
			*/
		}
		eeprom.setEepromRead(true);
		return eeprom;
	}

	public static String[] size2id(Context context, int length) throws IOException {
		DBHelper helper = new DBHelper(context);
		LinkedList<String> ret = new LinkedList<String>();
		SQLiteDatabase db = null;
		Cursor c = null;
		String name = null;
		try {
			db = helper.getReadableDatabase();
			String query = "SELECT name FROM eeprom WHERE size = " + length + " OR xsize = " + length + " ORDER BY name";
			c = db.rawQuery(query, null);
			while (c.moveToNext()) {
				name = c.getString(c.getColumnIndex("name"));
				ret.add(name);
			}
		} finally {
			if (c != null) {
				c.close();
			}
			if (db != null) {
				db.close();
			}
		}
		if (ret.size() == 0) {
			throw new IOException(context.getString(R.string.unable_to_determine_ecm_type));
		}

		Log.d(TAG, "EEPROM ID(s) from size: " + ret);
		return ret.toArray(new String[0]);
	}

	public class Page {
		private int nr;
		private int length;
		private int start;
		private boolean touched;

		public Page(int nr, int length) {
			this.nr = nr;
			this.length = length;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int nr() {
			return nr;
		}

		public int start() {
			return start;
		}

		public int length() {
			return length;
		}

		public EEPROM getParent() {
			return EEPROM.this;
		}

		public byte[] getBytes(int offset, int length, byte[] buffer, int buffer_pos) {
			data = getParent().getBytes();
			System.arraycopy(data, start + offset, buffer, buffer_pos, length);
			return buffer;
		}

		@Override
		public String toString() {
			return String.format(Locale.ENGLISH, "Page[#: %2d, start: %04X, length: %04X]", nr, start, length);
		}

		public void touch() {
			touched = true;
		}

		public void saved() {
			touched = false;
		}

		public boolean isTouched() {
			return touched;
		}
	}

	public int getPageCount() {
		return pages.size();
	}

	public Page getPage(int pageno) {
		for (Page page : pages) {
			if (page.nr == pageno) {
				return page;
			}
		}
		return null;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public int getXsize() {
		return xsize;
	}

	public boolean isEepromRead() {
		return eepromRead;
	}

	public void setEepromRead(boolean eepromRead) {
		this.eepromRead = eepromRead;
	}

	public void touch(int offset, int length) {
		// Mark page dirty
		for (Page pg : pages) {
			if (offset >= pg.start && offset < pg.start + pg.length) {
				pg.touch();
				// Log.d(TAG, pg + " dirty");
			}
		}
		touched = true;
	}

	public boolean isTouched() {
		return touched;
	}

	public void saved() {
		touched = false;
	}

	public boolean hasPageZero() {
		return length == xsize;
	}
}
