<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('ivalt'); section>
    <#if section="header">
        ${msg("ivaltVerifyTitle", "Verify iVALT Setup")}
    <#elseif section="form">
        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcLabelWrapperClass!}">
                <p>${msg("ivaltVerifyInstructions", "A verification notification has been sent to your mobile device")}</p>
                <p><strong>${msg("ivaltMobileNumber", "Mobile")}: ${mobileNumber!}</strong></p>
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcInputWrapperClass!}">
                <div class="ivalt-waiting-indicator">
                    <div class="spinner"></div>
                    <p>${msg("ivaltWaitingVerification", "Please approve the notification on your device...")}</p>
                </div>
            </div>
        </div>

        <form id="kc-ivalt-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="action" value="verify" />
            
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}">
                        ${msg("ivaltCheckVerification", "Check Verification")}
                    </button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="action" value="cancel">
                        ${msg("doCancel")}
                    </button>
                </div>
            </div>
        </form>

        <script type="text/javascript">
            // Auto-refresh every 10 seconds to check verification status
            setTimeout(function() {
                // Add hidden input to indicate this is a verification check
                var form = document.getElementById('kc-ivalt-verify-form');
                var actionInput = document.createElement('input');
                actionInput.type = 'hidden';
                actionInput.name = 'action';
                actionInput.value = 'verify';
                form.appendChild(actionInput);
                form.submit();
            }, 10000);
        </script>

        <style>
            .ivalt-waiting-indicator {
                text-align: center;
                padding: 20px;
            }
            .spinner {
                border: 4px solid #f3f3f3;
                border-top: 4px solid #3498db;
                border-radius: 50%;
                width: 40px;
                height: 40px;
                animation: spin 1s linear infinite;
                margin: 0 auto 15px;
            }
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        </style>
    </#if>
</@layout.registrationLayout>
