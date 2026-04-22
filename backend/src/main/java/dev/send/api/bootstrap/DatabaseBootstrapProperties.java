package dev.send.api.bootstrap;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "database-bootstrap")
public class DatabaseBootstrapProperties {
  private boolean enabled = true;
  private Path dataDirectory = Path.of("../data");

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Path getDataDirectory() {
    return dataDirectory;
  }

  public void setDataDirectory(Path dataDirectory) {
    this.dataDirectory = dataDirectory == null ? Path.of("../data") : dataDirectory;
  }
}
