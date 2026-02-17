<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('ivalt'); section>
    <#if section="header">
        ${msg("ivaltAuthTitle", "iVALT Authentication")}
    <#elseif section="form">
        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcLabelWrapperClass!}">
                <p>${msg("ivaltAuthInstructions", "A notification has been sent to your mobile device")}</p>
                <p><strong>${msg("ivaltMobileNumber", "Mobile")}: ${mobileNumber!}</strong></p>
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!}">
            <div class="${properties.kcInputWrapperClass!}">
                <div class="ivalt-waiting-indicator">
                    <div class="spinner"></div>
                    <p>${msg("ivaltWaitingApproval", "Waiting for approval...")}</p>
                </div>
            </div>
        </div>

        <form id="kc-ivalt-auth-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="action" value="check" />
            
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login">
                        ${msg("ivaltCheckStatus", "Check Status")}
                    </button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="action" value="cancel">
                        ${msg("doCancel")}
                    </button>
                </div>
            </div>
        </form>

        <script type="text/javascript">
            // Auto-refresh every 3 seconds to check status
            setTimeout(function() {
                document.getElementById('kc-ivalt-auth-form').submit();
            }, 3000);
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
