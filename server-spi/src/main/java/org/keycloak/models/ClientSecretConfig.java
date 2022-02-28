package org.keycloak.models;
/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class ClientSecretConfig extends AbstractConfig{

  // client attribute names
  public static final String CLIENT_SECRET_ROTATION_ENABLED = "client.secret.rotation.enabled";
  public static final String CLIENT_SECRET_CREATION_TIME = "client.secret.creation.time";
  public static final String CLIENT_SECRET_EXPIRATION = "client.secret.expiration.time";
  public static final String CLIENT_ROTATED_SECRET = "client.secret.rotated";
  public static final String CLIENT_ROTATED_SECRET_CREATION_TIME = "client.secret.rotated.creation.time";
  public static final String CLIENT_ROTATED_SECRET_EXPIRATION_TIME = "client.secret.rotated.expiration.time";

  public static final String CLIENT_SECRET_EXPIRED = "client.secret.expired";
  public static final String CLIENT_ROTATED_SECRET_EXPIRED = "client.secret.rotated.expired";

}
