package pl.devsite.system;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
@Slf4j
public class SystemProcess {

	private Process processHandle;
	private OutputStream processIn;
	private InputStream processStd;
	private InputStream processErr;
	private String processName;

	public SystemProcess(String processPath, String[] processOptions) throws IOException {
		String[] array = new String[processOptions.length + 1];
		array[0] = processPath;
		System.arraycopy(processOptions, 0, array, 1, processOptions.length);
		processHandle = Runtime.getRuntime().exec(array);
		StringBuilder cmd = new StringBuilder();
		for (String s : array) {
			cmd.append(s).append(' ');
		}
		processName = cmd.length() > 0 ? cmd.substring(0, cmd.length() - 1) : null;
		initializeStreams();
	}

	public SystemProcess(String processPath, String processOptions) throws IOException {
		String command = "" + processPath + (processOptions != null && !processOptions.isEmpty() ? " " + processOptions : "");
		processHandle = Runtime.getRuntime().exec(command);
		processName = command;
		initializeStreams();
	}

	private void initializeStreams() {
		processStd = processHandle.getInputStream();
		processErr = processHandle.getErrorStream();

		processIn = processHandle.getOutputStream();
	}

	protected SystemProcess() {
	}

	public void kill() {
		processHandle.destroy();
	}

	public SystemProcess flush() throws IOException {
		processIn.flush();
		return this;
	}

	@Override
	public String toString() {
		return (processName != null ? processName + "" : "");
	}

	public static void pump(InputStream inputStream, OutputStream outputStream) throws IOException {
		if (inputStream == null) {
			return;
		}
		byte[] buf = new byte[1024];
		int count;
		do {
			count = inputStream.read(buf);
			if (count > 0) {
				outputStream.write(buf, 0, count);
			}
		} while (count > 0);
		outputStream.flush();
		inputStream.close();
		outputStream.close();
	}

	public static void pumpBackground(final InputStream inputStream, final OutputStream outputStream) throws IOException {
		Thread t = new Thread(() -> {
			try {
				pump(inputStream, outputStream);
			} catch (IOException ex) {
				log.error("", ex);
			}
		});
		t.start();
	}

	public Process getProcessHandle() {
		return processHandle;
	}

	public InputStream getProcessErr() {
		return processErr;
	}

	public OutputStream getProcessIn() {
		return processIn;
	}

	public InputStream getProcessStd() {
		return processStd;
	}

	public SystemProcess pipe(SystemProcess process) throws IOException {
		pump(this.getProcessStd(), process.getProcessIn());
		return process;
	}

	public SystemProcess pipeBackground(SystemProcess process) throws IOException {
		pumpBackground(this.getProcessStd(), process.getProcessIn());
		return process;
	}

	public void pipe(OutputStream outputStream) throws IOException {
		pump(this.getProcessStd(), outputStream);
	}

	public void pipeBackground(final OutputStream outputStream) throws IOException {
		pumpBackground(this.getProcessStd(), outputStream);
	}
}
