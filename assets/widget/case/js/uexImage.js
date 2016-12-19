/**
 * Created by ylt on 16/8/23.
 */

if (UNIT_TEST) {


    var uexImageCase = {
        "openPicker": function () {
            var data = {
                min: 2,
                max: 3,
                quality: 0.8,
                detailedInfo: true,
                "viewFramePicPreview":{   //位置、大小
                    x:0,
                    y:0,
                    w:720, //w:1080,
                    h:1137, //h:1767
                },
                "style" : 1,
                "viewFramePicGrid":{   //位置、大小
                    x:0,
                    y:0,
                    w:720, //w:1080,
                    h:1137, //h:1767
                }
            }
            uexImage.openPicker(data, function (error, info) {
                if (error == -1) {
                    UNIT_TEST.log("取消操作");
                    UNIT_TEST.assert(true)
                } else if (error == 0) {
                    UNIT_TEST.log(info.data);
                    if (info.detailedImageInfo) {
                        UNIT_TEST.log(JSON.stringify(info.detailedImageInfo));
                    }
                    UNIT_TEST.assert(true);
                } else {
                    UNIT_TEST.assert(false);
                }
            });
        },
        "openBrowser":function () {
            var data ={
                displayActionButton:true,
                displayNavArrows:true,
                enableGrid:true,
                startIndex:2,
                data:["res://photo1.jpg",
                    {
                        src:"res://photo2.jpg",
                        thumb:"res://photo2t.jpg",
                        showBigPic: true
                    },
                    {
                        src:"res://photo3.jpg",
                        thumb:"res://photo3t.jpg",
                        desc:"22222222222222"
                    },
                    {
                        src:"http://pic.fxxz.com/up/2016-5/201654940195240.jpg",
                        thumb:"res://photo4t.jpg",
                        desc:"22222222222222"
                    }],
			    "style" : 1,
                "gridBackgroundColor" : "#4A88C1",  // style为1时生效
                "gridBrowserTitle" : "图片浏览",
            };
            uexImage.openBrowser(data,function(){
                UNIT_TEST.assert(true);
            });
        },
        "openCropper":function () {
            var data={
                src:"res://photo4.jpg",
                mode:2
            };
            uexImage.openCropper(data,function(error,info){
                if(error==-1){
                    UNIT_TEST.log("操作取消");
                    UNIT_TEST.assert(true);
                }else if(error==0) {
                    UNIT_TEST.log(info.data);
                    UNIT_TEST.assert(true);
                }
            });
        },
        "saveToPhotoAlbum":function () {
            var data={
                localPath:"res://photo4.jpg"
            };
            var json=JSON.stringify(data);
            uexImage.saveToPhotoAlbum(json,function(err,errStr){
                if(!err){
                    UNIT_TEST.assert(true);
                }else{
                    UNIT_TEST.assert(false);
                }
            });
        },
        "clearOutputImages":function () {
            var ret = uexImage.clearOutputImages();
            UNIT_TEST.assertTrue(ret);
        },
        "openLabelViewContainer": function() {
            var data = {
                "width": 600,
                "height": 333,
                "image":"res://photo2.jpg",
                "x":50,
                "y":500,
            }
            uexImage.openLabelViewContainer(data);
            UNIT_TEST.assert(true);
        },
        "addLabelView": function() {
           // var data = [{"id": 1, "content": "content", x: 20, y: 40}, {"id": 2, "content": "label content 2", x:50, y: 100}, {"id": 3, "content": "label content 3444", x:100, y: 200}]
            var data = {"id": 1, "content": "content", x: 20, y: 40};
            uexImage.addLabelView(JSON.stringify(data));
            UNIT_TEST.assert(true);
        },
        "getPicInfoWithLabelViews": function() {
            var data = uexImage.getPicInfoWithLabelViews();
            console.log("data:" + JSON.stringify(data));
            UNIT_TEST.assert(true);
        },
        "showLabelViewPic": function() {
            var data = {
                "width": 720,
                "height": 400,
                "image":"res://photo2.jpg",
                "x":0,
                "y":0,
                "labels": [
                    {
                      "id": "1",
                      "content": "content",
                      "left": "0.033",
                      "right": "0.358",
                      "top": "0.120",
                      "bottom": "0.276"
                    },
                    {
                      "id": "2",
                      "content": "label content 2",
                      "left": "0.083",
                      "right": "0.578",
                      "top": "0.300",
                      "bottom": "0.456",
                      "targetPointMode":0
                    },
                    {
                      "id": "3",
                      "content": "label content 3",
                      "left": "0.167",
                      "right": "0.752",
                      "top": "0.601",
                      "bottom": "0.757",
                      "targetPointMode":1
                    }
                ]
            };
            uexImage.showLabelViewPic(JSON.stringify(data));
            UNIT_TEST.assert(true);
        },
        "compressImage":function () {
            var params = {
                    srcPath : "/storage/emulated/0/DCIM/Camera/IMG_20161010_093830.jpg",
                    desLength : 30*1024
            };
            var data = JSON.stringify(params);
            uexImage.compressImage(data, function(err,errStr){
                if(1 == err) //成功
                {
                    alert(errStr);
                } else if (0 == err) //失败
                {
                    alert("error");
                }
            });
        }
    };

    UNIT_TEST.addCase("uexImageCase", uexImageCase);
}

