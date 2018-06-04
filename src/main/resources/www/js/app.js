var data = {
    defaultTimout: 10000,

    showSettingsDialog: false,

    showEditorDialog: false,
    editor: {
        mock: null,
        mockFilename: null
    },

    status: {
        mode: null,
        activeExpectations: [],
        recordedExpectations: [],
        enableBodyMatching: false,
        enableHeaderMatching: false
    },
    search: '',
    headers: [
        { text: '#', value: 'index' },
        { text: 'protocol', value: 'protocol' },
        { text: 'host', value: 'host' },
        { text: 'path', value: 'path' },
        { text: 'detail', value: 'detail' },
        { text: 'filename', value: 'filename' }
    ],
    snackbar: {
        show: false,
        text: ''
    }
};

Vue.component('page', {
  template: '#page'
});
new Vue({
    el: '#app',
    data: data,
    created: function () {
        timeout(data.defaultTimout, fetch('status'))
            .then(function(response) {
                return response.json().then(function(json) {
                    data.status = json;
                    document.getElementById("app").style.removeProperty("display");
                    document.getElementById("welcomeBanner").style.display = "none";
                });
            })
            .catch(function(error) {
                document.getElementById("app").style.removeProperty("display");
                document.getElementById("welcomeBanner").style.display = "none";
                console.error(error);
                data.snackbar.text = "An error occured, see logs for more information";
                data.snackbar.show = true;
            });
    },
    mounted: function() {

        var clipboardFilename = new ClipboardJS('.copyFileName');
        clipboardFilename.on('success', function(e) {
            showSnackbar("Filename copied to clipboard");
            console.info('Action:', e.action);
            console.info('Text:', e.text);
            console.info('Trigger:', e.trigger);

            e.clearSelection();
        });

        clipboardFilename.on('error', function(e) {
            showSnackbar("Unable to copied filename to clipboard");
            console.error('Action:', e.action);
            console.error('Trigger:', e.trigger);
        });
    },
    methods: {
        startRecording: function(event) {
            timeout(data.defaultTimout, fetch('record'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.snackbar.text = "Recording";
                        data.snackbar.show = true;
                        data.status = json;
                    });
                })
                .catch(function(error) {
                    console.error(error);
                    data.snackbar.text = "An error occured, see logs for more information";
                    data.snackbar.show = true;
                });
        },
        stopRecording: function(event) {
            timeout(data.defaultTimout, fetch('play'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.snackbar.text = "Mocks loaded";
                        data.snackbar.show = true;
                        data.status = json;
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    data.snackbar.text = "An error occured, see logs for more information";
                    data.snackbar.show = true;
                });
        },
        refreshStatus: function(event) {
            timeout(data.defaultTimout, fetch('status'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.snackbar.text = "Status refreshed";
                        data.snackbar.show = true;
                        data.status = json;
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    data.snackbar.text = "An error occured, see logs for more information";
                    data.snackbar.show = true;
                });
        },
        edit: function(id) {
            var self = this;
            timeout(data.defaultTimout, fetch('expectations/edit?id=' + id))
                .then(function(response) {
                    return response.json().then(function(json) {
                        self.displayEditor(id, json.mock, json.mockFilename);
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    data.snackbar.text = "An error occured, see logs for more information";
                    data.snackbar.show = true;
                });
        },
        displaySettings: function(event) {
            data.showSettingsDialog = true;


        },
        hideSettings: function(event) {
             var self = this;
             timeout(data.defaultTimout, fetch('updateStatus?enableHeaderMatching=' + data.status.enableHeaderMatching + "&enableBodyMatching=" + data.status.enableBodyMatching))
                 .then(function(response) {
                     return response.json().then(function(json) {
                         data.status = json;
                         data.showSettingsDialog = false;
                     })
                 })
                 .catch(function(error) {
                     console.error(error);
                     data.showSettingsDialog = false;
                     data.snackbar.text = "An error occured, see logs for more information";
                     data.snackbar.show = true;
                 });

        },
        displayEditor: function(id, text, filename) {
            data.editor.mock = text;
            data.editor.mockFilename = filename;

            data.showEditorDialog = true;
        },
        hideEditor: function(event) {
            data.showEditorDialog = false;
        }
    }
})


function showSnackbar(message) {
    data.snackbar.text = message;
    data.snackbar.show = true;
}

function timeout(ms, promise) {
  return new Promise(function(resolve, reject) {
    setTimeout(function() {
      reject(new Error("timeout"))
    }, ms)
    promise.then(resolve, reject)
  })
}