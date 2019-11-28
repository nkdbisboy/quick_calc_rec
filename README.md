## 如何分分钟实现速算题的自动批改
### 执行逻辑
这里选择使用android开发环境，执行流程很简单，如下图所示。
![c18c7d1033e75df485ead32456a8cabe.png](en-resource://database/905:1)
本示例支持拍照和本地图片选择两种模式，核心在于调用科大讯飞提供的“拍照速算识别”引擎，然后解析引擎返回的结果进行绘图展示即可。
### 环境准备
#### android开发环境
本示例在Android 9环境下顺利执行，Android studio使用版本3.5.2， Android studio的安装和使用这里不做过多介绍。
#### “拍照速算识别”引擎环境
科大讯飞提供的“拍照速算识别”引擎服务需要进行授权认证，所以我们需要先在开放平台上申请一个appid以备使用，申请流程如下：
1. 注册账号
登陆https://www.xfyun.cn/，点击右上角注册按钮按照提示流程完成注册。
2. 创建应用
登陆成功后，点击右上角“控制台”进入控制台页面
![f63c3c570611975ad775c68f4c5fd777.png](en-resource://database/907:1)
点击创建新应用
![180092a5b9c4d2a0c6cdf562458f2b51.png](en-resource://database/909:1)
填写应用的基本信息，点击提交即可创建应用成功。
3. 获取授权
在控制台点击左侧 “文字识别”-> "拍照速算识别"，记录下APPID、APISecret、APIKey备用。
![6bc94421e9c8a306a717f725dee14583.png](en-resource://database/911:1)
### 开发环节
#### 拍照并展示
调用相机的核心代码如下
```
// 启动相机程序
Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
startActivityForResult(intent, 1);

try {
// 将拍摄的照片显示出来    
Bitmap bitmap = 
BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
// Bitmap bitmap = image_itr(imageUri); 
picture.setImageBitmap(bitmap);
} 
catch (Exception e) 
{    
e.printStackTrace();
}
```
但这里有几个坑需要注意，
1. 在7.0以上系统中，Android不再允许在app中把file://Uri暴露给其他app，所以不能直接通过file:// 访问文件，需要使用官方给出的解决方案FileProvider
2. 需要提供相机和外部存储访问权限
#### 访问文件
```
//如果没有权限则申请权限
if (ContextCompat.checkSelfPermission(ImageAlbumShow.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
{ 
ActivityCompat.requestPermissions(ImageAlbumShow.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
}
//调用打开相册
openAlbum();

private void openAlbum() 
{ 
Intent intent = new Intent("android.intent.action.GET_CONTENT"); 
intent.setType("image/*"); 
startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
}
```
#### 拍照速算识别引擎调用
在获取到小学算数题的图片后，就可以开始调用拍照速算识别引擎了，在调用业务接口时，都需要在 Http Request Body 中配置以下参数，请求数据均为json字符串。
![30ff4739f569b3a47fb6485cebeb1e1d.png](en-resource://database/913:1)
但实际操作时不需要这么麻烦，官网（[https://www.xfyun.cn/services/photo-calculate-recg](https://www.xfyun.cn/services/photo-calculate-recg)）上提供了调用的示例代码，直接下载下来就可以使用，这里不做赘述，需要注意的是，示例代码中的星号部分需要替换为环境准备阶段申请的应用授权信息，如下所示：
```
    // ITR webapi 接口地址
    private static final String WebITR_URL = "https://rest-api.xfyun.cn/v2/itr"; //https url
    // 应用ID（到控制台获取）
    private static final String APPID = "*****";
    // 接口APIKey（到控制台的拍照速算识别页面获取）
    private static final String API_KEY = "*****";
    // 接口APISercet（到控制台的拍照速算识别页面获取）
    private static final String API_SECRET =  "*****";
    public static byte[] imageByteArray;
```
#### 解析识别结果
引擎返回结果为一个json结构，包含字段如下：
![a78d7182c93d2140ce0477d4c3f35a62.png](en-resource://database/915:1)
识别结果信息在data字段里，其中主要包含了识别出来的每个算数题的位置信息、算术题的识别文本结果、算数题判决正误信息。
![39474a1fc96ef35941c1b0fb43213dc0.png](en-resource://database/917:1)
![0a36e5e6a44b83400616c3ad7b5d3903.png](en-resource://database/919:1)
![8df0b300f6b4553772edeb8be56a378b.png](en-resource://database/921:1)
#### 绘图并展示
对于计算正确的算术题使用绿色方框显示，对于计算错误的算术题使用红色方框显示。
```
    private void displayImage(Bitmap image_bitmap) {
        if (imagePath != null) {
            Bitmap bitmap = null;
            try {
                // 绘制最终展示的图片
                bitmap = image_itr(image_bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // 将绘制好的图片展示出来
            picture.setImageBitmap(bitmap);
        }
        else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap image_itr(Bitmap image_bitmap){
        try {
            // 调用“拍照速算识别”AI能力
            WebITR.ResponseData responseData =  WebITR.call_itr(imagePath);
            // 处理返回的结果（这里未做异常处理，请自行添加）
            if (null != responseData){
                ArrayList imp_line_info = (ArrayList)((LinkedTreeMap)((LinkedTreeMap)((LinkedTreeMap)responseData.getData()).get("ITRResult")).
                                get("multi_line_info")).get("imp_line_info");
                for (int i=0; i< imp_line_info.size(); i++){
                    int rst = ((Double)((LinkedTreeMap)imp_line_info.get(i)).get("total_score")).intValue();
                    LinkedTreeMap imp_line_rect = (LinkedTreeMap)((LinkedTreeMap)imp_line_info.get(i)).get("imp_line_rect");
                    int x1 = ((Double) imp_line_rect.get("left_up_point_x")).intValue();
                    int y1 = ((Double ) imp_line_rect.get("left_up_point_y")).intValue();
                    int x2 = ((Double ) imp_line_rect.get("right_down_point_x")).intValue();
                    int y2 = ((Double ) imp_line_rect.get("right_down_point_y")).intValue();
                    // 正确的画绿框，错误的画红框
                    image_bitmap = draw_image(image_bitmap, x1,y1,x2,y2,rst);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image_bitmap;
    }

    private  Bitmap draw_image(Bitmap imageBitmap ,int x1, int y1, int x2, int y2, int result){
        Bitmap mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        if (result == 1)
            paint.setColor(Color.GREEN);
        else if (result == 0)
            paint.setColor(Color.RED);
        else return imageBitmap;
        paint.setStyle(Paint.Style.STROKE);//不填充
        paint.setStrokeWidth(2);  //线的宽度
        canvas.drawRect(x1, y1, x2, y2, paint);
        return     mutableBitmap;
    }
```
### 效果展示
至此，开发过程完成，让我们来体验一下程序的执行效果：
1. 点开主界面，这里为了方便展示，选择“相册”
![aaf67d1800557e347cafdbb786af2ef2.png](en-resource://database/938:0)
2. 选择一张事先拍好的小学算数题
![79fb4a0657d84d3af1c1b8e63a6ae587.png](en-resource://database/925:1)
3. 展示自动批改后的效果图
![a293406798752aeb3375eb8702412fff.png](en-resource://database/940:0)
### 完整代码获取
https://github.com/nkdbisboy/quick_calc_rec
注意，使用需要替换源码中APPID、APISecret、APIKey字段定义，获取方式见“环境准备”章节。
