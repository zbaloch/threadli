
// var HOST = "https://d13txem1unpe48.cloudfront.net/"
var HOST = "/upload/"

document.addEventListener("trix-attachment-add", function(event) {
    console.log('inside upload file')
    if (event.attachment.file) {
    uploadFileAttachment(event.attachment)
    }
})

function uploadFileAttachment(attachment) {
    uploadFile(attachment.file, setProgress, setAttributes)

    function setProgress(progress) {
    attachment.setUploadProgress(progress)
    }

    function setAttributes(attributes) {
    console.log(attributes);
    attachment.setAttributes(attributes)
    }
}

function uploadFile(file, progressCallback, successCallback) {
    var key = createStorageKey(file)
    var formData = createFormData(key, file)
    var xhr = new XMLHttpRequest()
console.log('inside file upload')
    xhr.open("POST", HOST + workspaceId, true)

    xhr.upload.addEventListener("progress", function(event) {
    var progress = event.loaded / event.total * 100
    progressCallback(progress)
    })

    xhr.addEventListener("load", function(event) {
    console.log(xhr)
    if (xhr.status == 204) {
        var attributes = {
        // url: HOST + key,
        // href: HOST + key + "?content-disposition=attachment"
        url: xhr.responseText,
        href: xhr.responseText + "?content-disposition=attachment"
        }
        successCallback(attributes)
    }
    if (xhr.status == 200) {
        var attributes = {
            url: xhr.responseText,
            href: xhr.responseText + "?content-disposition=attachment"
        }
        successCallback(attributes)
    }
    })

    xhr.send(formData)
}

function createStorageKey(file) {
    var date = new Date()
    var day = date.toISOString().slice(0,10)
    var name = date.getTime() + "-" + file.name
    return [ "tmp", day, name ].join("/")
}

function createFormData(key, file) {
    var data = new FormData()
    // data.append("key", key)
    data.append("Content-Type", file.type)
    data.append("file", file)
    return data
}