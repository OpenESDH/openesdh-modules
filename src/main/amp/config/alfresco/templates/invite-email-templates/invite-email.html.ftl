<html>
   <#assign inviterPersonRef=args["inviterPersonRef"]/>
   <#assign inviterPerson=companyhome.nodeByReference[inviterPersonRef]/>
   <#assign inviteePersonRef=args["inviteePersonRef"]/>
   <#assign inviteePerson=companyhome.nodeByReference[inviteePersonRef]/>

   <head>
      <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
      <style type="text/css"><!--
      body
      {
         font-family: Arial, sans-serif;
         font-size: 14px;
         color: #4c4c4c;
      }
      
      a, a:visited
      {
         color: #0072cf;
      }
      --></style>
   </head>
   
   <body bgcolor="#dddddd">
      <table width="100%" cellpadding="20" cellspacing="0" border="0" bgcolor="#dddddd">
         <tr>
            <td width="100%" align="center">
               <table width="70%" cellpadding="0" cellspacing="0" bgcolor="white" style="background-color: white; border: 1px solid #aaaaaa;">
                  <tr>
                     <td width="100%">
                        <table width="100%" cellpadding="0" cellspacing="0" border="0">
                           <tr>
                              <td style="padding: 10px 30px 0px;">
                                 <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                    <tr>
                                       <td>
                                          <table cellpadding="0" cellspacing="0" border="0">
                                             <tr>
                                                <td>
                                                   <div style="font-size: 22px; padding-bottom: 4px;">                                                      
                                                      ${message('invite.email.title', args["siteName"])}
                                                   </div>
                                                   <div style="font-size: 13px;">
                                                      ${date?datetime?string("dd-MM-yyyy hh:mm")}
                                                   </div>
                                                </td>
                                             </tr>
                                          </table>
                                          <div style="font-size: 14px; margin: 12px 0px 24px 0px; padding-top: 10px; border-top: 1px solid #aaaaaa;">
                                             <p>${message('invite.email.greeting')} ${inviteePerson.properties["cm:firstName"]!""},</p>
      
                                             <p>
                                                ${message('invite.email.has.invited.you.to.join', inviterPerson.properties["cm:firstName"]!"", inviterPerson.properties["cm:lastName"]!"", args["siteName"], args["inviteeSiteRole"])}
                                             </p>
                                             
                                             <p>${message('invite.email.click.to.accept', inviterPerson.properties["cm:firstName"]!"")}<br />
                                             <br /><a href="${args["acceptLink"]}">${args["acceptLink"]}</a></p>
                                             
                                             <#if args["inviteeGenPassword"]?exists>
                                             <p>${message('invite.email.account.created')}:<br />
                                             <br />${message('invite.email.username')}: <b>${args["inviteeUserName"]}</b>
                                             <br />${message('invite.email.password')}: <b>${args["inviteeGenPassword"]}</b>
                                             </p>
                                             
                                             <p><b>${message('invite.email.recommend.change.password')}</b><br />
                                             </#if>
                                             
                                             <p>${message('invite.email.click.to.decline', inviterPerson.properties["cm:firstName"]!"")}:<br />
                                             <br /><a href="${args["rejectLink"]}">${args["rejectLink"]}</a></p>
                                             
                                             <p>${message('invite.email.sincerely')},<br />
                                             ${message('opene.product.name')}</p>
                                          </div>
                                       </td>
                                    </tr>
                                 </table>
                              </td>
                           </tr>
                        </table>
                     </td>
                  </tr>
               </table>
            </td>
         </tr>
      </table>
   </body>
</html>