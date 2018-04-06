package artemis.messenger;

import java.io.IOException;
import java.io.InputStream;

import com.walkertribe.ian.PathResolver;

import android.content.res.AssetManager;

/**
 * Path resolver that points to app's Assets folder - needed for vesselData.xml.
 * @author Jordan Longstaff
 *
 */
public class AssetsResolver implements PathResolver {
	private final AssetManager manager;
	
	public AssetsResolver(AssetManager am) {
		manager = am;
	}

	@Override
	public InputStream get(String path) throws IOException {
		return manager.open(path);
	}

}
