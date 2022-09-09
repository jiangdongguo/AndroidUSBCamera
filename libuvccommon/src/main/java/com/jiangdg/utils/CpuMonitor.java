/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.jiangdg.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Modified 2016 t_saki@serenegiant.com
 *
 * Simple CPU monitor.  The caller creates a CpuMonitor object which can then
 * be used via sampleCpuUtilization() to collect the percentual use of the
 * cumulative CPU capacity for all CPUs running at their nominal frequency.  3
 * values are generated: (1) getCpuCurrent() returns the use since the last
 * sampleCpuUtilization(), (2) getCpuAvg3() returns the use since 3 prior
 * calls, and (3) getCpuAvgAll() returns the use over all SAMPLE_SAVE_NUMBER
 * calls.
 *
 * <p>CPUs in Android are often "offline", and while this of course means 0 Hz
 * as current frequency, in this state we cannot even get their nominal
 * frequency.  We therefore tread carefully, and allow any CPU to be missing.
 * Missing CPUs are assumed to have the same nominal frequency as any close
 * lower-numbered CPU, but as soon as it is online, we'll get their proper
 * frequency and remember it.  (Since CPU 0 in practice always seem to be
 * online, this unidirectional frequency inheritance should be no problem in
 * practice.)
 *
 * <p>Caveats:
 *   o No provision made for zany "turbo" mode, libuvccommon in the x86 world.
 *   o No provision made for ARM big.LITTLE; if CPU n can switch behind our
 *     back, we might get incorrect estimates.
 *   o This is not thread-safe.  To call asynchronously, create different
 *     CpuMonitor objects.
 *
 * <p>If we can gather enough info to generate a sensible result,
 * sampleCpuUtilization returns true.  It is designed to never through an
 * exception.
 *
 * <p>sampleCpuUtilization should not be called too often in its present form,
 * since then deltas would be small and the percent values would fluctuate and
 * be unreadable. If it is desirable to call it more often than say once per
 * second, one would need to increase SAMPLE_SAVE_NUMBER and probably use
 * Queue<Integer> to avoid copying overhead.
 *
 * <p>Known problems:
 *   1. Nexus 7 devices running Kitkat have a kernel which often output an
 *      incorrect 'idle' field in /proc/stat.  The value is close to twice the
 *      correct value, and then returns to back to correct reading.  Both when
 *      jumping up and back down we might create faulty CPU load readings.
 */

public final class CpuMonitor {
	private static final String TAG = "CpuMonitor";
	private static final int SAMPLE_SAVE_NUMBER = 10;  // Assumed to be >= 3.

	private int[] percentVec = new int[SAMPLE_SAVE_NUMBER];
	private int sum3 = 0;
	private int sum10 = 0;
	private long[] cpuFreq;
	private int cpusPresent;
	private double lastPercentFreq = -1;
	private int cpuCurrent;
	private int cpuAvg3;
	private int cpuAvgAll;
	private boolean initialized = false;
	private String[] maxPath;
	private String[] curPath;
	private final ProcStat lastProcStat = new ProcStat(0L, 0L);
	private final Map<String, Integer> mCpuTemps = new HashMap<String, Integer>();
	private int mTempNum = 0;
	private float tempAve = 0;

	private static final class ProcStat {
		private long runTime;
		private long idleTime;

		private ProcStat(final long aRunTime, final long aIdleTime) {
			runTime = aRunTime;
			idleTime = aIdleTime;
		}

		private void set(final long aRunTime, final long aIdleTime) {
			runTime = aRunTime;
			idleTime = aIdleTime;
		}

		private void set(final ProcStat other) {
			runTime = other.runTime;
			idleTime = other.idleTime;
		}
	}

	private void init() {
		try {
			final FileReader fin = new FileReader("/sys/devices/system/cpu/present");
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scanner = new Scanner(rdr).useDelimiter("[-\n]");
				scanner.nextInt();  // Skip leading number 0.
				cpusPresent = 1 + scanner.nextInt();
				scanner.close();
			} catch (final Exception e) {
				Log.e(TAG, "Cannot do CPU stats due to /sys/devices/system/cpu/present parsing problem");
			} finally {
				fin.close();
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Cannot do CPU stats since /sys/devices/system/cpu/present is missing");
		} catch (IOException e) {
			Log.e(TAG, "Error closing file");
		}

		cpuFreq = new long [cpusPresent];
		maxPath = new String[cpusPresent];
		curPath = new String[cpusPresent];
		for (int i = 0; i < cpusPresent; i++) {
			cpuFreq[i] = 0;  // Frequency "not yet determined".
			maxPath[i] = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
			curPath[i] = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
		}

		lastProcStat.set(0, 0);

		mCpuTemps.clear();
		mTempNum = 0;
		for (int i = 0; i < 50; i++) {
			final String path = "/sys/class/hwmon/hwmon" + i;
			final File dir = new File(path);
			if (dir.exists() && dir.canRead()) {
				mCpuTemps.put(path, 0);
				mTempNum++;
			}
		}

		initialized = true;
	}

	/**
	 * Re-measure CPU use.  Call this method at an interval of around 1/s.
	 * This method returns true on success.  The fields
	 * cpuCurrent, cpuAvg3, and cpuAvgAll are updated on success, and represents:
	 * cpuCurrent: The CPU use since the last sampleCpuUtilization call.
	 * cpuAvg3: The average CPU over the last 3 calls.
	 * cpuAvgAll: The average CPU over the last SAMPLE_SAVE_NUMBER calls.
	 * CPU周辺の温度を取得するのが少し重いのでUIスレッドからは呼び出さないほうが良い
	 */
	public boolean sampleCpuUtilization() {
		long lastSeenMaxFreq = 0;
		long cpufreqCurSum = 0;
		long cpufreqMaxSum = 0;

		if (!initialized) {
			init();
		}

		for (int i = 0; i < cpusPresent; i++) {
			/*
			 * For each CPU, attempt to first read its max frequency, then its
			 * current frequency.  Once as the max frequency for a CPU is found,
			 * save it in cpuFreq[].
			 */

			if (cpuFreq[i] == 0) {
				// We have never found this CPU's max frequency.  Attempt to read it.
				long cpufreqMax = readFreqFromFile(maxPath[i]);
				if (cpufreqMax > 0) {
					lastSeenMaxFreq = cpufreqMax;
					cpuFreq[i] = cpufreqMax;
					maxPath[i] = null;  // Kill path to free its memory.
				}
			} else {
				lastSeenMaxFreq = cpuFreq[i];  // A valid, previously read value.
			}

			long cpufreqCur = readFreqFromFile(curPath[i]);
			cpufreqCurSum += cpufreqCur;

			/* Here, lastSeenMaxFreq might come from
			 * 1. cpuFreq[i], or
			 * 2. a previous iteration, or
			 * 3. a newly read value, or
			 * 4. hypothetically from the pre-loop dummy.
			 */
			cpufreqMaxSum += lastSeenMaxFreq;
		}

		if (cpufreqMaxSum == 0) {
			Log.e(TAG, "Could not read max frequency for any CPU");
			return false;
		}

		/*
		 * Since the cycle counts are for the period between the last invocation
		 * and this present one, we average the percentual CPU frequencies between
		 * now and the beginning of the measurement period.  This is significantly
		 * incorrect only if the frequencies have peeked or dropped in between the
		 * invocations.
		 */
		final double newPercentFreq = 100.0 * cpufreqCurSum / cpufreqMaxSum;
		final double percentFreq = lastPercentFreq > 0 ? (lastPercentFreq + newPercentFreq) * 0.5 : newPercentFreq;
		lastPercentFreq = newPercentFreq;

		final ProcStat procStat = readIdleAndRunTime();
		if (procStat == null) {
			return false;
		}

		final long diffRunTime = procStat.runTime - lastProcStat.runTime;
		final long diffIdleTime = procStat.idleTime - lastProcStat.idleTime;

		// Save new measurements for next round's deltas.
		lastProcStat.set(procStat);

		final long allTime = diffRunTime + diffIdleTime;
		int percent = allTime == 0 ? 0 : (int) Math.round(percentFreq * diffRunTime / allTime);
		percent = Math.max(0, Math.min(percent, 100));

		// Subtract old relevant measurement, add newest.
		sum3 += percent - percentVec[2];
		// Subtract oldest measurement, add newest.
		sum10 += percent - percentVec[SAMPLE_SAVE_NUMBER - 1];

		// Rotate saved percent values, save new measurement in vacated spot.
		for (int i = SAMPLE_SAVE_NUMBER - 1; i > 0; i--) {
			percentVec[i] = percentVec[i - 1];
		}
		percentVec[0] = percent;

		cpuCurrent = percent;
		cpuAvg3 = sum3 / 3;
		cpuAvgAll = sum10 / SAMPLE_SAVE_NUMBER;

		tempAve = 0;
		float tempCnt = 0;
		for (final String path: mCpuTemps.keySet()) {
			final File dir = new File(path);
			if (dir.exists() && dir.canRead()) {
				final File file = new File(dir, "temp1_input");
				if (file.exists() && file.canRead()) {
					final int temp = (int)readFreqFromFile(file.getAbsolutePath());
					mCpuTemps.put(path, temp);
					if (temp > 0) {
						tempCnt++;
						tempAve += temp > 1000 ? temp / 1000.0f : temp;
					}
				}
			}
		}
		if (tempCnt > 0) {
			tempAve /= tempCnt;
		}
		return true;
	}

	public int getCpuCurrent() {
		return cpuCurrent;
	}

	public int getCpuAvg3() {
		return cpuAvg3;
	}

	public int getCpuAvgAll() {
		return cpuAvgAll;
	}

	public int getTempNum() {
		return mTempNum;
	}

	public int getTemp(final int ix) {
		int result = 0;
		if ((ix >= 0) && (ix < mTempNum)) {
			final String path = "/sys/class/hwmon/hwmon" + ix;
			if (mCpuTemps.containsKey(path)) {
				result = mCpuTemps.get(path);
			}
		}
		return result;
	}

	public float getTempAve() {
		return tempAve;
	}

	/**
	 * Read a single integer value from the named file.  Return the read value
	 * or if an error occurs return 0.
	 */
	private long readFreqFromFile(String fileName) {
		long number = 0;
		try {
			final FileReader fin = new FileReader(fileName);
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scannerC = new Scanner(rdr);
				number = scannerC.nextLong();
				scannerC.close();
			} catch (final Exception e) {
				// CPU presumably got offline just after we opened file.
			} finally {
				fin.close();
			}
		} catch (final FileNotFoundException e) {
			// CPU is offline, not an error.
		} catch (final IOException e) {
			Log.e(TAG, "Error closing file");
		}
		return number;
	}

	/*
	 * Read the current utilization of all CPUs using the cumulative first line
	 * of /proc/stat.
	 */
	private ProcStat readIdleAndRunTime() {
		long runTime = 0;
		long idleTime = 0;
		try {
			final FileReader fin = new FileReader("/proc/stat");
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scanner = new Scanner(rdr);
				scanner.next();
				long user = scanner.nextLong();
				long nice = scanner.nextLong();
				long sys = scanner.nextLong();
				runTime = user + nice + sys;
				idleTime = scanner.nextLong();
				scanner.close();
			} catch (final Exception e) {
				Log.e(TAG, "Problems parsing /proc/stat");
				return null;
			} finally {
				fin.close();
			}
		} catch (final FileNotFoundException e) {
			Log.e(TAG, "Cannot open /proc/stat for reading");
			return null;
		} catch (final IOException e) {
			Log.e(TAG, "Problems reading /proc/stat");
			return null;
		}
		return new ProcStat(runTime, idleTime);
	}
}
