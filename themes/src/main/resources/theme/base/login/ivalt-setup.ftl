<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mobileNumber','countryCode'); section>
    <#if section="header">
        ${msg("ivaltSetupTitle", "Configure iVALT MFA")}
    <#elseif section="form">
        <form id="kc-ivalt-setup-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="countryCode" class="${properties.kcLabelClass!}">${msg("ivaltCountryCode", "Country Code")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <select id="countryCode" name="countryCode" class="${properties.kcInputClass!}" required>
                        <option value="">${msg("ivaltSelectCountry", "Select Country")}</option>
                        <#list countryCodes as code>
                            <#assign parts = code?split(":")>
                            <option value="${parts[0]}">${parts[0]} - ${parts[1]}</option>
                        </#list>
                    </select>
                    <#if messagesPerField.existsError('countryCode')>
                        <span id="input-error-country-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('countryCode'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="mobileNumber" class="${properties.kcLabelClass!}">${msg("ivaltMobileNumber", "Mobile Number")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input id="mobileNumber" name="mobileNumber" type="tel" class="${properties.kcInputClass!}" 
                           placeholder="${msg("ivaltMobileNumberPlaceholder", "Enter your mobile number")}"
                           required autofocus
                           aria-invalid="<#if messagesPerField.existsError('mobileNumber')>true</#if>" />
                    <#if messagesPerField.existsError('mobileNumber')>
                        <span id="input-error-mobile-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('mobileNumber'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}">
                    <p class="ivalt-help-text">${msg("ivaltSetupHelp", "You will receive a verification notification on your mobile device.")}</p>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="action" value="submit">
                        ${msg("ivaltSendVerification", "Send Verification")}
                    </button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="action" value="cancel">
                        ${msg("doCancel")}
                    </button>
                </div>
            </div>
        </form>

        <style>
            .ivalt-help-text {
                font-size: 0.9em;
                color: #666;
                margin-top: 10px;
            }
        </style>
    </#if>
</@layout.registrationLayout>
