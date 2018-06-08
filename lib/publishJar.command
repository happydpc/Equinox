mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=/Users/aurora/Documents/Developer/Libraries/base64-data-encryption/base64-all.jar \
  -DgroupId=com.base64 \
  -DartifactId=base64-all \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/Users/aurora/EclipseWorkspace/Equinox/lib

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=/Users/aurora/EclipseWorkspace/AppContainer/appContainer.jar \
  -DgroupId=com.container \
  -DartifactId=container \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/Users/aurora/EclipseWorkspace/Equinox/lib

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=/Users/aurora/Documents/Developer/Libraries/CustomControls/customControls.jar \
  -DgroupId=com.controls \
  -DartifactId=controls \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/Users/aurora/EclipseWorkspace/Equinox/lib

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=/Users/aurora/Documents/Developer/Libraries/INF/oofem/inf-oofem-all.jar \
  -DgroupId=com.inf \
  -DartifactId=inf \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/Users/aurora/EclipseWorkspace/Equinox/lib

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=/Users/aurora/EclipseWorkspace/EquinoxServer/equinoxServer.jar \
  -DgroupId=com.equinox-server \
  -DartifactId=equinox-server \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=/Users/aurora/EclipseWorkspace/Equinox/lib