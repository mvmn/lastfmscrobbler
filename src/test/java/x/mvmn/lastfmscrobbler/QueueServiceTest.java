package x.mvmn.lastfmscrobbler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import x.mvmn.lastfmscrobbler.model.TrackLogEntry;

public class QueueServiceTest {

	@Test
	public void test() {
		File file = new File(System.getProperty("java.io.tmpdir"));
		QueueService queue = new QueueService(file);
		if (queue.queueFile.exists()) {
			queue.queueFile.delete();
		}
		TrackLogEntry[] mockTracks = new TrackLogEntry[5];
		for (int i = 0; i < 5; i++) {
			mockTracks[i] = TrackLogEntry.builder()
					.artist("Test Artist")
					.album("Album")
					.title("Test " + i)
					.duration(123)
					.timestamp(678)
					.build();
			queue.queueTrack(mockTracks[i]);
		}
		List<TrackLogEntry> processedTracks = new ArrayList<>();
		queue.processQueuedTracks(processedTracks::addAll, 3);

		Assert.assertEquals(3, processedTracks.size());
		Assert.assertEquals(mockTracks[4], processedTracks.get(0));
		Assert.assertEquals(mockTracks[3], processedTracks.get(1));
		Assert.assertEquals(mockTracks[2], processedTracks.get(2));

		processedTracks.clear();
		queue.processQueuedTracks(processedTracks::addAll, 3);

		Assert.assertEquals(2, processedTracks.size());
		Assert.assertEquals(mockTracks[1], processedTracks.get(0));
		Assert.assertEquals(mockTracks[0], processedTracks.get(1));
		queue.queueFile.delete();
	}
}
