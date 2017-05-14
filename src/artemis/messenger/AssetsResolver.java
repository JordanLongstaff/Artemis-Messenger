package artemis.messenger;

import java.io.IOException;
import java.io.InputStream;

import com.walkertribe.ian.vesseldata.PathResolver;

import android.content.res.AssetManager;

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
