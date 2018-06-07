var data = {
    defaultTimout: 10000,

    showSettingsDialog: false,

    mockSetSelector : {
        searchMockSet: null,
        show: false
    },

    showEditorDialog: false,
    editor: {
        id: null,
        mock: null,
        mockSha1: null,
        mockFilename: null
    },
    settings: {
        enableHeaderMatching: null,
        enableBodyMatching: null
    },
    status: {
        mocksSet: [],
        currentMockSet: null,
        mode: null,
        activeExpectations: [],
        recordedExpectations: [],
        enableHeaderMatching: false,
        enableBodyMatching: false
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
        var self = this;
        var clipboardFilename = new ClipboardJS('.copyFileName');
        clipboardFilename.on('success', function(e) {
            self.showSnackbar("Filename copied to clipboard");
            console.info('Action:', e.action);
            console.info('Text:', e.text);
            console.info('Trigger:', e.trigger);

            e.clearSelection();
        });

        clipboardFilename.on('error', function(e) {
            self.showSnackbar("Unable to copied filename to clipboard");
            console.error('Action:', e.action);
            console.error('Trigger:', e.trigger);
        });
    },
    methods: {
        showSnackbar: function(message) {
            this.snackbar.text = message;
            this.snackbar.show = true;
        },

        startRecording: function(event) {
            var self = this;
            timeout(data.defaultTimout, fetch('record'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.status = json;
                        self.showSnackbar("Recording");
                    });
                })
                .catch(function(error) {
                    console.error(error);
                    self.showSnackbar("An error occured, see logs for more information");
                });
        },
        stopRecording: function(event) {
            var self = this;
            timeout(data.defaultTimout, fetch('play'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.status = json;
                        self.showSnackbar("Mocks loaded");
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    self.showSnackbar("An error occured, see logs for more information");
                });
        },
        refreshStatus: function(event) {
            var self = this;
            timeout(data.defaultTimout, fetch('status'))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.status = json;
                        self.showSnackbar("Status refreshed");
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    self.showSnackbar("An error occured, see logs for more information");
                });
        },
        edit: function(id, filename) {
            var self = this;
            timeout(data.defaultTimout, fetch('expectations/edit?filename=' + filename))
                .then(function(response) {
                    return response.json().then(function(json) {
                        self.displayEditor(id, json.mock, json.mockFilename);
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    self.showSnackbar("An error occured, see logs for more information");
                });
        },
        updateCurrentMockSet: function(value) {
            var self = this;
            self.mockSetSelector.show = false;
            this
                .updateStatus(data.status.enableHeaderMatching, data.status.enableBodyMatching, value)
                .then(function() {
                    self.stopRecording();
                });
        },
        updateStatus: function(enableHeaderMatching, enableBodyMatching, mockSet) {
            var self = this;
            return timeout(data.defaultTimout, fetch('updateStatus?enableHeaderMatching=' + enableHeaderMatching + "&enableBodyMatching=" + enableBodyMatching + "&mockSet=" + mockSet))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.status = json;
                        data.showSettingsDialog = false;
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    data.showSettingsDialog = false;
                    self.showSnackbar("An error occured, see logs for more information");
                });
        },

        displaySettings: function(event) {
            data.showSettingsDialog = true;
            data.settings.enableHeaderMatching = data.status.enableHeaderMatching;
            data.settings.enableBodyMatching = data.status.enableBodyMatching;
        },
        hideSettings: function(event) {
            var self = this;

            if (data.settings.enableHeaderMatching == data.status.enableHeaderMatching
                &&  data.settings.enableBodyMatching == data.status.enableBodyMatching) {
                data.showSettingsDialog = false;
                return;
            }

            self.updateStatus(data.settings.enableHeaderMatching, data.settings.enableBodyMatching, data.status.currentMockSet)

        },

        displayEditor: function(id, text, filename) {
            data.editor.id = id;
            data.editor.mock = text;
            data.editor.mockSha1 = sha1(text);
            data.editor.mockFilename = filename;

            data.showEditorDialog = true;
        },
        hideEditor: function(event) {
            var self = this;

            if (data.editor.mockSha1 == sha1(data.editor.mock)) {
                data.showEditorDialog = false;
                return;
            }

            try {
                JSON.parse(data.editor.mock);
            } catch (error) {
                self.showSnackbar("Not a valid JSON object");
                return;
            }

            timeout(data.defaultTimout,
                fetch('expectations/update?id=' + data.editor.id + "&filename=" + data.editor.mockFilename, {
                    method: "POST",
                    body: data.editor.mock
                }))
                .then(function(response) {
                    return response.json().then(function(json) {
                        data.status = json;
                        data.showEditorDialog = false;
                    })
                })
                .catch(function(error) {
                    console.error(error);
                    data.showEditorDialog = false;
                    self.showSnackbar("An error occured, see logs for more information");
                });

        }
    },
    computed: {
        filteredSearchMockSet: function() {
            var self = this;
            return self.status.mocksSet.filter(function(mockSet) {
                return self.mockSetSelector.searchMockSet == null || mockSet.toLowerCase().indexOf(self.mockSetSelector.searchMockSet.toLowerCase()) > -1
            });
        }
    }
});

function timeout(ms, promise) {
  return new Promise(function(resolve, reject) {
    setTimeout(function() {
      reject(new Error("timeout"))
    }, ms);
    promise.then(resolve, reject)
  })
}