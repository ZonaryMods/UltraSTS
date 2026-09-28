package com.kingpixel.ultrasts.configs;

import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.models.STS;
import lombok.Data;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class STSConf {
  private STSConf() {}

  public static final Map<String, STS> STS_MAP = new ConcurrentHashMap<>();

  public static void init() {
    STS_MAP.clear();
    Path path = UltraSTS.getPath().resolve("sts");
    if (path.toFile().exists()) {
      try {
        var files = UtilsFile.getAllJsonFiles(path);
        for (var file : files) {
          STS sts = UtilsFile.read(file, STS.class);
          if (sts != null) {
            String id = file.getFileName().toString().replace(".json", "");
            sts.setId(id);
            UtilsFile.writeAsync(file, sts);
            STS_MAP.put(id, sts);
          }
        }
      } catch (Exception e) {
        UltraSTS.LOGGER.error("Failed to load STS configurations", e);
      }
    } else {
      path.toFile().mkdirs();
      STS defaultSTS = new STS();
      defaultSTS.setId("default");
      UtilsFile.writeAsync(path.resolve("default.json"), defaultSTS);
      STS_MAP.put("default", defaultSTS);
    }

  }

  public static STS getSTS(String id) {
    if (id != null && !id.isEmpty()) {
      STS sts = STS_MAP.get(id);
      if (sts != null) return sts;
    }
    return STS_MAP.values().stream().findFirst().orElse(null);
  }

  public static int getSize() {
    return STS_MAP.size();
  }
}
