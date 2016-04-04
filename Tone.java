import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

public class Tone {
	public enum Channel {
		LEFT, RIGHT, STEREO
			};

	public static final float SAMPLE_RATE = 44104; // Should be a multiple of 8
	protected byte[] buf;
	protected int hz, msecs;
	protected double vol;
	protected Channel channel;

	Tone() {
	} // necessary so that subclasses don't complain

	public Tone(int hz, int msecs, double vol, Tone.Channel channel) {
		if (hz <= 0)
			throw new IllegalArgumentException("Frequency <= 0 hz");
		if (msecs <= 0)
			throw new IllegalArgumentException("Duration <= 0 msecs");
		if (vol > 1.0 || vol < 0.0)
			throw new IllegalArgumentException("Volume out of range 0.0 - 1.0");
		this.channel = channel;
		this.hz = hz;
		this.vol = vol;
		this.msecs = msecs;
		generateTone();

	}

	private void generateTone() {
		int len = (int)Math.ceil((2 * SAMPLE_RATE * msecs / 1000.0d));
		if (len % 2 == 1)
			len = len + 1;
		buf = new byte[len];
		int fadeCount = 1600;
		for (int i = 0; i < buf.length /2; i++) {
			double fadeRate = 1.0;
			double angle = (i * hz / SAMPLE_RATE) * 2.0 * Math.PI;
			if (i<fadeCount) {
				fadeRate = (double)i/(double)fadeCount;
			} else if (i>(buf.length/2)-fadeCount) {
				int bufLength = buf.length;
				int buf = bufLength/2;
				int countDown = buf-i;
				fadeRate = (double)countDown/(double)(fadeCount);
			}
			buf[2*i + 1] = buf[2*i] = (byte) Math.round(
														Math.cos(angle) * 127.0 * vol * fadeRate);
		}
	}
	/*
	private void generateTone() {
		int len = (int)Math.ceil((2 * SAMPLE_RATE * msecs / 1000.0d));
		if (len % 2 == 1)
			len = len + 1;
		buf = new byte[len];
		for (int i = 0; i < buf.length /2; i++) {
			double angle = (i * hz / SAMPLE_RATE) * 2.0 * Math.PI;
			buf[2*i + 1] = buf[2*i] = (byte) Math.round(Math.sin(angle) * 127.0 * vol);
		}
	}
	*/
	public void play(SourceDataLine sdl) { // takes an opened SourceDataLine
		FloatControl panControl = (FloatControl) sdl
            .getControl(FloatControl.Type.PAN);
		if (panControl != null) { // Preferred method using built in sound
			// control, but not guaranteed to be
			// available
			if (channel == Channel.LEFT) {
				panControl.setValue(-1);
			} else if (channel == Channel.RIGHT) {
				panControl.setValue(1);
			} else {
				panControl.setValue(0);
			}
		} else { // fallback method is directly manipulates the buffer
			if (channel != Channel.STEREO) {
				int nSilenceOffset;
				byte nSilenceValue = 0;
				if (channel == Channel.LEFT) {
					nSilenceOffset = 1;
				} else {
					nSilenceOffset = 0;
				}
				for (int i = 0; i < buf.length; i += 2) {
					buf[i + nSilenceOffset] = nSilenceValue;
				}
			}

		}

		sdl.write(buf, 0, buf.length);

	}


	public void playSilence(SourceDataLine sdl,
							int msecSilence) {
		// takes an opened SourceDataLine

		/* Write some silence at the end */
		final int silenceSamples = 
			(int)(Tone.SAMPLE_RATE * msecSilence/1000.0 * 2.0);
		byte [] silenceBuf = new byte[silenceSamples];
		for (int i=0; i<silenceSamples; i++)
			silenceBuf[i] = 0;

		sdl.write(silenceBuf, 0, silenceBuf.length);

	}

	public static void main(String[] args) throws Exception {
		AudioFormat af = new AudioFormat(Tone.SAMPLE_RATE, 8, 2, true, false);
		SourceDataLine sdl;
		try {
			sdl = AudioSystem.getSourceDataLine(af);
		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Couldn't get sound line");
			return;
		}
		try {
			sdl.open(af);
		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(null, "Couldn't open sound line");
			return;
		}

		System.out.println("Before sleep 1");
		Thread.sleep(1000);
		System.out.println("After sleep 1");

		sdl.start();

		System.out.println("Before sleep 2");
		Thread.sleep(1000);
		System.out.println("After sleep 2");

		Tone left = new Tone(400, 2000, .5, Tone.Channel.LEFT);
		System.out.println("Playing left");
		long t = System.currentTimeMillis();
		left.playSilence(sdl,1000);
		left.play(sdl);
		left.playSilence(sdl,1000);
		System.out.println(System.currentTimeMillis()-t);
		System.out.println("Finished left");
		Tone right = new Tone(400, 2000, .5, Tone.Channel.RIGHT);
		System.out.println("Playing right");
		right.play(sdl);
		right.playSilence(sdl,1000);
		System.out.println("Finished right");
		sdl.drain();
		System.out.println("Finished drain");
		Thread.sleep(1000);
		sdl.stop();
		System.out.println("Finished stop");
		Thread.sleep(1000);
		sdl.close();
		System.out.println("Finished close");
		sdl = null;
	}

}