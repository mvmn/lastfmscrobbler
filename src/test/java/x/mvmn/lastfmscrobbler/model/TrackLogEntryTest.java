package x.mvmn.lastfmscrobbler.model;

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

public class TrackLogEntryTest {

	@Test
	public void testSerialization() {
		TrackLogEntry t1 = TrackLogEntry.builder().artist("test value").title("mock & test").duration(12345).timestamp(34567).build();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		t1.serialize(baos);
		TrackLogEntry t2 = TrackLogEntry.deserialize(baos.toByteArray());
		Assert.assertEquals(t1, t2);
	}
}
