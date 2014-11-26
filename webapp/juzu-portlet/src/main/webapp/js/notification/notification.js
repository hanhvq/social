(function(sUtils, $) {

  var Notification = {
    formData: '',
    parentId: '#userNotification',
    saveSetting : function(e) {
      var jElm = $(this);
      if(jElm.is('button') && jElm.hasClass('disabled')) {
        return;
      }
      var id = jElm.attr('id');
      var msgOk = jElm.attr('data-ok');
      var msgNOk = jElm.attr('data-nok');
      
      Notification.formData = $(document.forms['uiNotificationSetting']).serialize();
      $(Notification.parentId).jzAjax({        
        url : "UserNotificationSetting.saveSetting()",
        data : {
          "params" : Notification.formData
        },
        success : function(data) {
          if(data.ok === 'true') {
            if(data.status === 'false') {
              $(Notification.parentId).find('div.form-horizontal:first').hide();
            } else {
              $(Notification.parentId).find('div.form-horizontal:first').show();
            }
          }
          if(jElm.is('button')) {
            jElm.addClass('disabled');
          }
        }
      }).fail(function(jqXHR, textStatus) {
        alert( "Request failed: " + textStatus + ". "+jqXHR);
      });
    },
    onload : function() {
      Notification.formData = $(document.forms['uiNotificationSetting']).serialize();
      var parent = $(Notification.parentId);
      var save = parent.find("button#Save").addClass('disabled');
      var reset = parent.find("button#Reset");
      //
      save.on('click', Notification.saveSetting) ;
      //
      reset.on('click', function(e) {
        var elm = $(this);
        var close = elm.parents('div:first').attr('data-close');
        var confTitle = elm.parents('div:first').attr('data-conf');
        var actions = {
          action: function() {
            $(Notification.parentId).jzAjax({        
              url : "UserNotificationSetting.resetSetting()",
              data : {},
              success : function(data) {
                var content = $('<div></div>').html(data).find('div.uiUserNotificationPortlet:first').html();
                $(Notification.parentId).html(content);
                Notification.onload();
              }
            }).fail(function(jqXHR, textStatus) {
              alert( "Request failed: " + textStatus + ". "+jqXHR);
            });
          }, 
          label: elm.attr('data-confirm-label')
        };
        sUtils.PopupConfirmation.confirm(elm.attr('id'), [actions], confTitle, elm.attr('data-confirm'), close);
      });
      //
      var horizontal = parent.find('div.form-horizontal');
      horizontal.find('input[type=checkbox]').on('click', Notification.checkActiveButton);
      horizontal.find('select').on('change', Notification.checkActiveButton);
      
      //
      var buttons = $("div.inputContainer");
	  buttons.on('click', function(e) {
		var input = $(this).find('input.providerAction');
		Notification.switchStatus(input.attr('name'), input.hasClass("false"));
	  });
    },
    switchStatus : function(pluginId, isEnable) {
      Notification.formData = $(document.forms['uiNotificationSetting']).serialize();
      $(Notification.parentId).jzAjax({   
	    url : "UserNotificationSetting.saveNotificationTypeSetting()",
	    data : {
	      "params" : Notification.formData,
	      "pluginId" : pluginId,
	      "enable" : isEnable
	    },
	    success : function(data) {
	      var clazz = (data.isEnable === true) ? 'enable' : 'disable';
	      var plugin = $("tr#" + data.pluginId);
	      plugin.attr("class", clazz);
	      var action = $('input[name=' + data.pluginId + ']')
	      action.attr('class', 'providerAction yesno ' + clazz);
	    }
	  }).fail(function(jqXHR, textStatus) {
	    alert("Request failed: " + textStatus + ". " + jqXHR);
	  });
	},
    checkActiveButton : function(e) {
      var newData = $(document.forms['uiNotificationSetting']).serialize();
      var parent = $(Notification.parentId);
      if (Notification.formData !== newData) {
        parent.find("button#Save").removeClass('disabled');
        parent.find("button#Reset").removeClass('disabled');
      } else if (parent.find("button#Save").hasClass('disabled') === false) {
        parent.find("button#Save").addClass('disabled');
      }
    }
  };
  Notification.onload();
  return Notification;
})(socialUtil, jq);
