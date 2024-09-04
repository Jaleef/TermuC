package cn.rbc.termuc;
import android.content.*;
import android.os.*;
import java.io.*;
import java.nio.charset.*;
import android.util.*;
import java.util.*;
import android.widget.*;
import android.app.*;
import android.view.*;
import android.graphics.drawable.*;
import android.net.*;
import android.content.pm.*;
import cn.rbc.codeeditor.util.*;
import android.app.AlertDialog.Builder;

public class Utils {
	public final static File ROOT = Environment.getExternalStorageDirectory();
	public final static String PREF = "/data/data/com.termux/files";
    private final static String PREFC = "com.termux.RUN_COMMAND_";

	public static void run(Context cont, String cmd, String[] args, String pwd, boolean background) {
		Intent it = new Intent();
		it.setClassName("com.termux", "com.termux.app.RunCommandService");
		it.setAction("com.termux.RUN_COMMAND");
		it.putExtra(new StringBuilder(PREFC).append("PATH").toString(), cmd);
		it.putExtra(new StringBuilder(PREFC).append("RUNNER").toString(), "app-shell");
		it.putExtra(new StringBuilder(PREFC).append("ARGUMENTS").toString(), args);
		it.putExtra(new StringBuilder(PREFC).append("WORKDIR").toString(), pwd);
		it.putExtra(new StringBuilder(PREFC).append("BACKGROUND").toString(), background);
		it.putExtra(new StringBuilder(PREFC).append("SESSION_ACTION").toString(), "0");
        cont.startService(it);
	}

	public static String escape(String str) {
        return str.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$").replace("\"", "\\\"");
    }

	public static boolean isBlob(File f) {
		int i = Math.min((int)f.length(), 2048);
		try {
			FileInputStream file = new FileInputStream(f);
			byte[] bArr = new byte[i];
			new DataInputStream(file).readFully(bArr);
			for (int i2 = 0; i2 < i; i2++) {
				if (bArr[i2] == 0) {
					file.close();
					return true;
				}
			}
			file.close();
		} catch (IOException e) {}
		return false;
	}

	public static boolean removeFiles(File dir) {
		File[] fl = dir.listFiles();
		if (fl!=null)
			for (File f:fl)
				removeFiles(f);
		return dir.delete();
	}

	public static void testApp(Activity ctx, boolean manually) {
		PackageManager pm = ctx.getPackageManager();
		try {
			pm.getPackageInfo("com.termux", PackageManager.GET_GIDS);
			if (manually)
				HelperUtils.show(Toast.makeText(ctx, R.string.installed, Toast.LENGTH_SHORT));
		} catch (PackageManager.NameNotFoundException e) {
			Builder bd = new Builder(ctx);
			bd.setTitle(R.string.install_app);
			bd.setMessage(R.string.confirm_install);
			bd.setNegativeButton(android.R.string.cancel, null);
			Install oc = new Install(ctx);
			bd.setPositiveButton(android.R.string.ok, oc);
			if (!manually)
				bd.setNeutralButton(R.string.no_remind, oc);
			bd.create().show();
		}
	}

	private static class Install
	implements DialogInterface.OnClickListener, SimpleAdapter.ViewBinder {
		Activity mApp;
		Install(Activity app) {
			mApp = app;
		}
		public boolean setViewValue(View v, Object o, String k) {
			if (v instanceof ImageView && o instanceof Drawable) {
				((ImageView)v).setImageDrawable((Drawable)o);
				return true;
			}
			return false;
		}
		public void onClick(DialogInterface d, int p) {
			Activity app = mApp;
			if (p==DialogInterface.BUTTON_NEUTRAL) {
				app.getPreferences(Activity.MODE_PRIVATE).edit().putBoolean(MainActivity.TESTAPP, false).commit();
				return;
			}
			Uri uri = Uri.parse("market://details?id=com.termux");
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			PackageManager pm = app.getPackageManager();
			List<ResolveInfo> lst = pm.queryIntentActivities(it, 0);
			List<Map<String,Object>> list = new ArrayList<>();
			Map<String,Object> m;
			final Intent[] its = new Intent[lst.size()+2];
			int i = 0;
			for (ResolveInfo ri:lst) {
				m = new ArrayMap<>();
				m.put("i", ri.loadIcon(pm));
				m.put("n", ri.loadLabel(pm));
				list.add(m);
				it = new Intent(Intent.ACTION_VIEW, uri);
				it.setPackage(ri.activityInfo.packageName);
				its[i++] = it;
			}
			m = new ArrayMap<>();
			m.put("i", null);
			m.put("n", "Github Release website");
			list.add(m);
			its[i++] = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-app/releases"));
			m = new ArrayMap<>();
			m.put("i", null);
			m.put("n", "F-Droid website");
			list.add(m);
			its[i] = new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux"));
			Builder bd = new Builder(app);
			bd.setTitle(R.string.install_via);
			bd.setNegativeButton(android.R.string.cancel, null);
			SimpleAdapter sadp = new SimpleAdapter(app, list, R.layout.file_item, new String[]{"i", "n"}, new int[]{R.id.file_icon, R.id.file_name});
			sadp.setViewBinder(this);
			bd.setAdapter(sadp, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface d, int p) {
						mApp.startActivity(its[p]);
					}
				});
			bd.create().show();
		}
	}

	public static void initBack(Activity ctx, boolean manually) {
		Builder bd = new Builder(ctx);
		bd.setTitle(R.string.init_termux);
		bd.setMessage(R.string.init_inform);
		Init it = new Init(ctx);
		bd.setPositiveButton(R.string.copy_jump, it);
		bd.setNegativeButton(android.R.string.cancel, null);
		if (!manually)
			bd.setNeutralButton(R.string.initialized, it);
		bd.create().show();
	}

	public static void copyJump(Context ctx) {
		ClipboardManager cm = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
		try {
			BufferedInputStream is = new BufferedInputStream(ctx.getAssets().open("init"));
			StringBuilder sb = new StringBuilder();
			int i;
			byte[] bt = new byte[1024];
			while ((i=is.read(bt))>0)
				sb.append(new String(bt, 0, i));
			is.close();
			ClipData cd = ClipData.newPlainText("Label", sb.toString());
			cm.setPrimaryClip(cd);
			Intent it = new Intent(Intent.ACTION_MAIN);
			it.setPackage("com.termux");
			ctx.startActivity(it);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static class Init implements DialogInterface.OnClickListener {
		Activity mCtx;
		public Init(Activity ctx) {
			mCtx = ctx;
		}
		public void onClick(DialogInterface p1, int p2) {
			if (p2 == DialogInterface.BUTTON_POSITIVE) {
				copyJump(mCtx);
			} else {
				mCtx.getPreferences(Activity.MODE_PRIVATE).edit().putBoolean(MainActivity.INITAPP, false).commit();
			}
		}
	}
}
