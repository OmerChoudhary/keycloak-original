<#macro termsAcceptance>
    <#if termsAcceptanceRequired??>
        <div class="form-group">
            <div class="${properties.kcInputWrapperClass!}">
                <#if msg("customTermsTitle") != "customTermsTitle">
                    ${msg("customTermsTitle")}
                <#else>
                    ${msg("termsTitle")}
                </#if>
                <div id="kc-registration-terms-text">
                    <#if msg("customTermsText") != "customTermsText">
                        ${kcSanitize(msg("customTermsText"))?no_esc}
                    <#else>
                        ${kcSanitize(msg("termsText"))?no_esc}
                    </#if>
                </div>
            </div>
        </div>
        <div class="form-group">
            <div class="${properties.kcLabelWrapperClass!}">
                <input type="checkbox" id="termsAccepted" name="termsAccepted" class="${properties.kcCheckboxInputClass!}"
                       aria-invalid="<#if messagesPerField.existsError('termsAccepted')>true</#if>"
                />
                <label for="termsAccepted" class="${properties.kcLabelClass!}">${msg("acceptTerms")}</label>
            </div>
            <#if messagesPerField.existsError('termsAccepted')>
                <div class="${properties.kcLabelWrapperClass!}">
                            <span id="input-error-terms-accepted" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('termsAccepted'))?no_esc}
                            </span>
                </div>
            </#if>
        </div>
    </#if>
</#macro>
