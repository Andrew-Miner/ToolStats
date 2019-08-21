package plugins.smokyminer.toolstats.statsection;

interface VerifyTracking<T>
{
	boolean isTracked(T item);
}

public class TrackWord<T> implements VerifyTracking<T>
{
	String word;
	VerifyTracking<T> verifier;
	
	public TrackWord(String word, VerifyTracking<T> verifier)
	{
		this.word = word;
		this.verifier = verifier;
	}
	
	public boolean isTracked(T item)
	{
		return verifier.isTracked(item);
	}
}
