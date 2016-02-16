package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.util.Time;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonWebToken implements Serializable {
    @com.fasterxml.jackson.annotation.JsonProperty("jti")
    @JsonProperty("jti")
    protected String id;
    @com.fasterxml.jackson.annotation.JsonProperty("exp")
    @JsonProperty("exp")
    protected int expiration;
    @com.fasterxml.jackson.annotation.JsonProperty("nbf")
    @JsonProperty("nbf")
    protected int notBefore;
    @com.fasterxml.jackson.annotation.JsonProperty("iat")
    @JsonProperty("iat")
    protected int issuedAt;
    @com.fasterxml.jackson.annotation.JsonProperty("iss")
    @JsonProperty("iss")
    protected String issuer;
    @com.fasterxml.jackson.annotation.JsonProperty("aud")
    @JsonProperty("aud")
    protected String audience;
    @com.fasterxml.jackson.annotation.JsonProperty("sub")
    @JsonProperty("sub")
    protected String subject;
    @com.fasterxml.jackson.annotation.JsonProperty(("typ"))
    @JsonProperty("typ")
    protected String type;
    @com.fasterxml.jackson.annotation.JsonProperty("azp")
    @JsonProperty("azp")
    public String issuedFor;
    protected Map<String, Object> otherClaims = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public JsonWebToken id(String id) {
        this.id = id;
        return this;
    }


    public int getExpiration() {
        return expiration;
    }

    public JsonWebToken expiration(int expiration) {
        this.expiration = expiration;
        return this;
    }

    @JsonIgnore
    public boolean isExpired() {
        return Time.currentTime() > expiration;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public JsonWebToken notBefore(int notBefore) {
        this.notBefore = notBefore;
        return this;
    }


    @JsonIgnore
    public boolean isNotBefore() {
        return Time.currentTime() >= notBefore;

    }

    /**
     * Tests that the token is not expired and is not-before.
     *
     * @return
     */
    @JsonIgnore
    public boolean isActive() {
        return (!isExpired() || expiration == 0) && (isNotBefore() || notBefore == 0);
    }

    public int getIssuedAt() {
        return issuedAt;
    }

    /**
     * Set issuedAt to the current time
     */
    @JsonIgnore
    public JsonWebToken issuedNow() {
        issuedAt = Time.currentTime();
        return this;
    }

    public JsonWebToken issuedAt(int issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }


    public String getIssuer() {
        return issuer;
    }

    public JsonWebToken issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }


    public String getAudience() {
        return audience;
    }

    public JsonWebToken audience(String audience) {
        this.audience = audience;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public JsonWebToken subject(String subject) {
        this.subject = subject;
        return this;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public JsonWebToken type(String type) {
        this.type = type;
        return this;
    }

    /**
     * OAuth client the token was issued for.
     *
     * @return
     */
    public String getIssuedFor() {
        return issuedFor;
    }

    public JsonWebToken issuedFor(String issuedFor) {
        this.issuedFor = issuedFor;
        return this;
    }

    /**
     * This is a map of any other claims and data that might be in the IDToken.  Could be custom claims set up by the auth server
     *
     * @return
     */
    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }
}
