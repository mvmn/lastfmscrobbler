package x.mvmn.lastfmscrobbler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import x.mvmn.lastfmscrobbler.model.TrackLogEntry;
import x.mvmn.lastfmscrobbler.util.FileHelper;

public class QueueService {

	protected static final Object QUEUE_LOCK = new Object();
	protected final File queueFile;

	public QueueService(File dataFolder) {
		this.queueFile = new File(dataFolder, "queue.dat");
	}

	public void queueTrack(TrackLogEntry track) {
		synchronized (QUEUE_LOCK) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			track.serialize(baos);
			try {
				FileHelper.appendToFile(queueFile, baos.toByteArray());
			} catch (IOException e) {
				// TODO: log
				e.printStackTrace();
			}
		}
	}

	public void processQueuedTracks(Consumer<List<TrackLogEntry>> processor, int max) {
		synchronized (QUEUE_LOCK) {
			if (queueFile.exists()) {
				try {
					List<TrackLogEntry> entries = new ArrayList<>();
					long lastOffset = -1;
					long fileSize = FileUtils.sizeOf(queueFile);
					if (fileSize > 0) {
						lastOffset = fileSize;
						while (max-- > 0 && lastOffset > 0) {
							long offset = lastOffset - 2;
							if (offset < 0) {
								offset = 0;
							}
							byte[] data;
							try (RandomAccessFile raf = new RandomAccessFile(queueFile, "r")) {
								raf.seek(offset);
								while (raf.read() != TrackLogEntry.END_OF_ENTRY_MARKER && offset > 0) {
									offset--;
									raf.seek(offset);
								}
								int entrySize = (int) (lastOffset - offset - 1);
								if (offset == 0) {
									raf.seek(0);
									entrySize = (int) lastOffset;
								}
								data = new byte[entrySize];
								raf.read(data, 0, data.length);
							}
							TrackLogEntry tle = TrackLogEntry.deserialize(data);
							entries.add(tle);
							lastOffset = offset;
						}
					}
					if (!entries.isEmpty()) {
						processor.accept(entries);
						FileHelper.truncateFile(queueFile, lastOffset);
					}
				} catch (Exception e) {
					// TODO: log
					e.printStackTrace();
				}
			}
		}
	}
}
