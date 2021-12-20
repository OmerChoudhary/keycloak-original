    <#import "template.ftl" as layout>
    <@layout.registrationLayout; section>
    <#if section = "title">
     title
    <#elseif section = "header">
        ${kcSanitize(msg("webauthn-login-title"))?no_esc}
    <#elseif section = "form">
        <div id="kc-form-webauthn" class="${properties.kcFormClass!}">
            <form id="webauth" action="${url.loginAction}" method="post">
                <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                <input type="hidden" id="authenticatorData" name="authenticatorData"/>
                <input type="hidden" id="signature" name="signature"/>
                <input type="hidden" id="credentialId" name="credentialId"/>
                <input type="hidden" id="userHandle" name="userHandle"/>
                <input type="hidden" id="error" name="error"/>
            </form>

            <div class="${properties.kcFormGroupClass!} no-bottom-margin">
                <#if authenticators??>
                    <#if authenticators.authenticators?size gt 1>
                        <p class="${properties.kcSelectAuthListItemTitle!}">${kcSanitize(msg("webauthn-available-authenticators"))}</p>
                    </#if>

                    <form id="authn_select" class="${properties.kcFormClass!}">
                        <#list authenticators.authenticators as authenticator>
                            <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
                            <div class="${properties.kcSelectAuthListItemClass!}">

                                <div class="${properties.kcSelectAuthListItemIconClass!}">
                                    <i class="${properties.kcWebAuthnKeyIcon} fa-2x"></i>
                                </div>
                                <div class="${properties.kcSelectAuthListItemBodyClass!}">
                                    <div id="kc-webauthn-authenticator-label" class="${properties.kcSelectAuthListItemHeadingClass!}">
                                        ${msg('${authenticator.label}')}
                                    </div>
                                    <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                        ${msg('webauthn-createdAt-label')} ${authenticator.createdAt}
                                    </div>
                                </div>
                                <div class="${properties.kcSelectAuthListItemFillClass!}"></div>
                            </div>
                        </#list>
                    </form>
                </#if>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input id="authenticateWebAuthnButton" type="button" onclick="webAuthnAuthenticate()"
                           value="${kcSanitize(msg("webauthn-doAuthenticate"))}"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"/>
                </div>
            </div>
        </div>

    <script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
    <script type="text/javascript" src="${url.resourcesPath}/js/base64url.js"></script>
    <script type="text/javascript">
        function webAuthnAuthenticate() {
            let isUserIdentified = ${isUserIdentified};
            if (!isUserIdentified) {
                doAuthenticate([]);
                return;
            }
            checkAllowCredentials();
        }

        function checkAllowCredentials() {
            let allowCredentials = [];
            let authn_use = document.forms['authn_select'].authn_use_chk;

            if (authn_use !== undefined) {
                if (authn_use.length === undefined) {
                    allowCredentials.push({
                        id: base64url.decode(authn_use.value, {loose: true}),
                        type: 'public-key',
                    });
                } else {
                    for (let i = 0; i < authn_use.length; i++) {
                        allowCredentials.push({
                            id: base64url.decode(authn_use[i].value, {loose: true}),
                            type: 'public-key',
                        });
                    }
                }
            }
            doAuthenticate(allowCredentials);
        }


    function doAuthenticate(allowCredentials) {

        // Check if WebAuthn is supported by this browser
        if (!window.PublicKeyCredential) {
            $("#error").val("${msg("webauthn-unsupported-browser-text")?no_esc}");
            $("#webauth").submit();
            return;
        }

        let challenge = "${challenge}";
        let userVerification = "${userVerification}";
        let rpId = "${rpId}";
        let publicKey = {
            rpId : rpId,
            challenge: base64url.decode(challenge, { loose: true })
        };

        let createTimeout = ${createTimeout};
        if (createTimeout !== 0) publicKey.timeout = createTimeout * 1000;

        if (allowCredentials.length) {
            publicKey.allowCredentials = allowCredentials;
        }

        if (userVerification !== 'not specified') publicKey.userVerification = userVerification;

        navigator.credentials.get({publicKey})
            .then((result) => {
                window.result = result;

                let clientDataJSON = result.response.clientDataJSON;
                let authenticatorData = result.response.authenticatorData;
                let signature = result.response.signature;

                $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), { pad: false }));
                $("#authenticatorData").val(base64url.encode(new Uint8Array(authenticatorData), { pad: false }));
                $("#signature").val(base64url.encode(new Uint8Array(signature), { pad: false }));
                $("#credentialId").val(result.id);
                if(result.response.userHandle) {
                    $("#userHandle").val(base64url.encode(new Uint8Array(result.response.userHandle), { pad: false }));
                }
                $("#webauth").submit();
            })
            .catch((err) => {
                $("#error").val(err);
                $("#webauth").submit();
            })
        ;
    }

    </script>
    <#elseif section = "info">

    </#if>
    </@layout.registrationLayout>
