package io.kurumi.ntt.fragment.tests;

import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.fragment.tests.MMPITest.Test;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.core.util.StrUtil;

public class MMPITest extends Fragment {

	final String POINT_TEST = "mmpi";
	
	@Override
	public void init(BotFragment origin) {
		
		super.init(origin);
		
		registerFunction("mmpi");
		
		registerPoint(POINT_TEST);
		
	}
	
	class Test extends PointData {
		
		int index = -1;
		
		boolean[] answer = new boolean[questions.length];
		
	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {
		
		String message = "请尽快填写你看完题目后的第一印象，不要在每一道题目上费太多时间思索。答案无所谓对与不对，好与不好，完全不必有任何顾虑。";

		setPrivatePoint(user,POINT_TEST,new Test());

		msg.send(message).keyboardHorizontal("开始").withCancel().async();
				
	}

	@Override
	public void onPoint(UserData user,Msg msg,String point,PointData data) {
		
		Test test = (Test) data;
		
		if (test.index == test.answer.length - 1) {
			
			test.answer[test.index] = "男".equals(msg.text());
			
			// 提交
			
			HttpRequest request = HttpUtil.createPost(POST);

			request.form("test_name",user.name());
			request.form("test_email","noreply@kurumi.io");
			request.form("hr_email","");
			
			request.form("checkbox","checkbox");
			
			for (int index = 0;index < test.answer.length;index ++) {
				
				request.form("answer" + (index + 1),test.answer[index] ? 1 : 0);
				
			}
			
			String resultId = StrUtil.subBetween(request.execute().body(),"id=","\"");

			msg.send("查看结果 : http://apesk.com/mensa/common_report_getid/mmpi_report_admin_m.asp?id=" + resultId).async();
			
		}
		
		if (test.index != -1) {
			
			test.answer[test.index] = "是".equals(msg.text());
			
		}

		test.index ++;
		
		if (test.index == test.answer.length - 1) {
			
			msg.send(questions[test.index]).keyboardHorizontal("男","女").async();
			
		} else {
			
			msg.send(questions[test.index]).keyboardHorizontal("是","否").async();
			
		}
		
	}
	
	public static String POST = "https://www.apesk.com/mensa/common_submit_hr/submit_mmpi_conn_m.asp";
	
	public static String[] questions = new String[] {

		"我喜欢看科技方面的网站",
		"我经常馋点东西吃",
		"我早上起来感觉精力充沛",
		"我想我会喜欢管理员的工作",
		"我睡眠很浅",
		"我喜欢研究犯罪心理",
		"即使在冬天我也很少感觉手脚发冷",
		"我很容易对新事物好奇",
		"我现在工作（学习）的能力，和从前差不多",
		"经常有如鲠在喉的感觉",
		"喜欢研究梦境带来的启发",
		"我喜欢看侦探电影",
		"我总是很容易紧张",
		"我每个月总有那么几次拉肚子",
		"偶尔我会臆想一些坏得说不出口的话",
		"我深信生活本质是残酷的",
		"我的父亲是一个善良的人",
		"我很少便秘",
		"当我干一件新的工作时，总喜欢别人告诉我，我应该和谁套近乎",
		"我有愉快的性生活",
		"有时我非常想离家出走",
		"有时会处于癫狂状态，连自己也不能控制",
		"有恶心和呕吐的毛病",
		"感觉很孤单，没有一个人了解我",
		"我想当一个表演艺术家",
		"当我处境困难的时候，我觉得最好是独自担起来",
		"有时我觉得有鬼附身",
		"人若犯我，我必犯人",
		"经常烧心的毛病使我苦恼",
		"有时我真想骂人",
		"每隔几个晚上我就做恶梦",
		"我发现我很难把注意力集中到一件事情上",
		"我曾经有过很特别，很奇怪的体验",
		"我时常咳嗽",
		"假如不是有人和我作对，我一定会有更大的成就",
		"我很少担心自己的健康",
		"我从来没有出轨",
		"我曾干过小偷小摸的事",
		"有时我真想砸东西",
		"有很多时候我宁愿坐着空想，而不愿做任何事情",
		"我曾一连几天，几个星期，几个月什么也不想干，因为总是提不起精神",
		"我家里人对我已选择的工作（或将要选择的职业）不满意",
		"我睡得不安，容易被惊醒",
		"我觉得我的头到处都疼",
		"有时我也说假话",
		"我的判断力比以往任何时候都好",
		"每星期至少有一、二次，我突然觉得无缘无故地全身发热",
		"当我与人相处的时候听到别人谈论希奇古怪的事，我就心烦",
		"最好是把所有的法律全都不要",
		"有时我觉得我的灵魂离开的我的身体",
		"我的身体和我大多数朋友一样地健康",
		"遇到同学或不常见朋友除非他们先向我打招呼，不然我就装作没看见",
		"祈祷可以治病",
		"大家都喜欢我",
		"我从来没有因为胸部痛或心痛而感到苦恼",
		"我小时候，曾经因为胡闹而受过学校的处分",
		"是自来熟的性格",
		"相信一切事情，老天自有安排",
		"我时常听从某些人的指挥，但其实他们还不如我高明",
		"我不是每天都看网站上的每一篇评论",
		"我从未有过正常的生活",
		"我身体某些部分常有像火烧、刺痛、虫爬、麻木的感觉",
		"我的大便正常，不难控制",
		"有时我会不停在做一件事情直到别人不耐烦为止",
		"我爱我的母亲",
		"我能在我周围看到其他人所看不到的东西，动物和人",
		"我希望我能象别人那样快乐",
		"我从未感到脖子（颈）后面疼痛",
		"和我性别相同的人对我有强烈的吸引力",
		"我过去经常喜欢玩丢手帕的游戏",
		"我觉得许多人喜欢夸大看书的不幸，以便得到别人的同情和帮助",
		"我为每隔几天或经常感到心口（胃）不舒服而烦恼",
		"我是个重要人物",
		"男性：我总希望我是女的，女性：我从不因为我是女的遗憾",
		"我有时发怒",
		"我时常感到悲观失望",
		"我看爱情小说",
		"我喜欢诗",
		"我的感情不容易受伤害",
		"我有时捉弄动物",
		"我想我会喜欢干森林管理员那一类的工作",
		"和人争辩的时候，我常争不过别人",
		"任何人只要他有能力，而且愿意努力工作是能成功的",
		"近来，我觉得很容易放弃对某些事物的希望",
		"有时我对别人的东西，如鞋、手套、等所强烈吸引，虽然这些东西对我毫无用处，但我总想摸 摸它或把它偷来",
		"我确实缺少自信心",
		"我愿意做一名园丁",
		"我总觉得人生是有价值的",
		"要使大数人相信事实的真相，是要经过一番辩论的",
		"有时我将今天应该做的事，拖到明天去做",
		"我不在乎别人拿我开玩笑",
		"我想当个护士",
		"我觉得大多数人是为了向上爬而不惜说谎的",
		"许多事情，我做过以后就后悔了",
		"我几乎每星期都去拜拜菩萨",
		"我几乎没有和家里人吵过嘴",
		"有时我有一种强烈的冲动，去做一些惊人或有害的事",
		"我相信善有善报，恶有恶报。不是不报时候未到。",
		"我喜欢参加热闹的聚会",
		"我碰到一些千头万绪的问题，使我感到犹豫不决",
		"我认为女的在性生活方面，应该和男的有同等的自由",
		"我认为最难的是控制我自己",
		"我很少有肌肉抽筋或颤抖的毛病",
		"我似乎对什么事情都无所谓",
		"我身体不舒服的时候，容易发脾气",
		"我总觉得我自己好象做错了什么事或犯了什么罪",
		"我经常是快乐的",
		"我时常觉得头胀鼻塞似的",
		"有些人太霸道，即使我明知他们是对的，也要和他们对着干",
		"有人想害我",
		"我从来没有为寻求刺激而去做危险的事",
		"我时常认为必须坚持那些我认为正确的事",
		"我相信法制",
		"我常觉得头上好像有一根绷得紧紧的带子",
		"我相信人死后还会有“来世”",
		"我更喜欢我下了赌注的比赛和游戏",
		"大部分人之所以是诚实的，主要是因为怕被人识破",
		"我在上学的时候，有时因胡闹而被校领导叫去",
		"我说话总是那样不快也不慢，不含糊也不嘶哑",
		"我在外边和朋友们一起吃饭的时候，比在家里规矩得多",
		"我相信有人暗中算计我",
		"我似乎和我周围的人一样精明能干",
		"我相信有人在跟踪我",
		"大多数人不惜用不正当的手段谋取利益，而不愿失掉机会",
		"我的胃有各种毛病",
		"我喜欢看综艺片",
		"我知道我烦恼的根源",
		"看到血的时候，我既不怕，也不难受",
		"我自己时常弄不清为什么会这样爱生气和发牢骚",
		"我从来没有吐过血或咯过血",
		"我从不杞人忧天，也不讳疾忌医",
		"我喜欢摆弄花花草草",
		"我从来没有过放荡淫乱的生活",
		"我才思敏捷，天马行空",
		"遇到一些能占小便宜又不会被发现的事情，我可能会去做",
		"如果别人待我好，我会怀疑他们别有用心",
		"我相信我的家庭生活，和我所认识的许多人一样幸福和谐",
		"批评和指责都使我非常伤心",
		"有时我仿佛觉得我必须伤害自己或别人",
		"我喜欢下厨",
		"我的行为多半受我周围人约定俗成的习惯和观念所支配",
		"有时我觉得我真是一无是处",
		"小时候我曾加入过一个帮派，有福共享，有难同当",
		"如果有机会到部队锻炼我会愿意参加",
		"有时我想无事生非别人打架",
		"我喜欢到处乱逛，如果被束缚了，我就不开心",
		"由于我经常不够果断而失去许多好机会",
		"当我正在做一件重要事情的时候，如果有人向我请教或打扰我，我会不耐烦的",
		"我以前写过日记",
		"玩游戏的时候，我只愿赢而不愿输",
		"有人一直想害死我",
		"大多数晚上，我睡觉时不受什么思想干扰",
		"近几年来，大部分时间我的身体都很棒",
		"我从来没有过抽疯或发癫的毛病",
		"现在我的体重很稳定",
		"有一段时间，过去事情的记忆似乎全忘了",
		"我觉得我时常无缘无故地挨整",
		"我容易情绪化",
		"我的人生观和世界观发生了动摇",
		"在我一生中，我从来没有感觉到象现在这么舒坦自在",
		"有时候我觉得我的头一碰就疼",
		"我痛恨别人以不正当的手段对付我",
		"我精力充沛不知疲倦",
		"我喜欢研究和上网搜索与我目前工作有关的东西",
		"我喜欢结识一些重要人物，这样会使我感到自己也变得举足轻重",
		"我有点恐高",
		"即使我家里有人犯法，我也不会紧张",
		"我的脑子有点毛病",
		"我有点理财头脑",
		"我不在乎别人怎么看我",
		"在聚会当中，我不喜欢出风头",
		"我时常需要努力使自己不显出怯场的样子",
		"我过去喜欢上学",
		"我从来没有昏倒过",
		"我很少头昏眼花",
		"我不大怕蛇或蜈蚣之类的东西",
		"我母亲是和善之人",
		"我的记忆力似乎还不错",
		"有关性方面的问题，使我烦恼",
		"我觉得我遇到生人的时候就不知道说什么好了",
		"无聊的时候，我就会惹事寻开心",
		"我怕自己会发疯",
		"我反对把钱给乞丐",
		"我时常听到说话的声音，而又不知道它从何而来",
		"我的听觉还不错",
		"当我要做一件事的时候，我常发觉我的手在发抖",
		"我的双手灵巧",
		"我能阅读很长的时间眼睛也不觉得累",
		"我经常觉得浑身无力",
		"我很少头痛",
		"当我难为情的时候，会出很多的汗，这使我非常苦恼",
		"走路时保持平稳，我并不困难",
		"我没哮喘这一类病",
		"我曾经有过几次突然不能控制自己的行动或言语，但当时我的头脑还很清醒",
		"我所认识的人里不是个个我都喜欢",
		"我喜欢到我从来没有到过的地方去游览",
		"有人一直想抢我的东西",
		"我很少空想",
		"我们应该把有关“性”方面的常识告诉孩子",
		"有人想窃取我的idea",
		"但愿我不象现在这样的害羞",
		"我相信我是一个被谴责的人",
		"假若我是一个新闻记者，我将喜欢报导戏剧界的新闻",
		"我喜欢做一个新闻记者",
		"有时我控制不住想要偷一点东西",
		"我很信神，程度超过多数人",
		"我喜欢许多不同种类的游戏和娱乐",
		"我喜欢和异性说笑",
		"我相信我的罪恶是不可饶恕的",
		"每一种东西吃起来味道都是一样",
		"我白天能睡觉，晚上却睡不着",
		"我家里的人把我当做小孩子，而不把我当做大人看待",
		"走路时，我很小心地跨过人行道上的接缝",
		"我从来没有为皮肤上长点东西而烦恼",
		"我曾经饮酒过度",
		"和别人的家庭比较，我的家庭缺乏爱和温暖",
		"我时常感到自己在为某些事而担忧",
		"当我看到动物受折磨的时候，我并不觉得特别难受",
		"我想我会喜欢建筑承包的工作",
		"我爱我的母亲",
		"我喜欢科学",
		"即使我以后不能报答恩惠，我也愿向朋友求助",
		"我很喜欢打猎",
		"我父母经常反对那些和我交往的人",
		"有时我也会说说人家的闲话",
		"我家里有些人的习惯，使我非常讨厌",
		"人家告诉我，我在睡觉中起来走路（梦游）",
		"有时我觉得我非常容易地做出决定",
		"我喜欢同时参加几个团体",
		"我从来没有感到心慌气短",
		"我喜欢谈论两性方面的事",
		"我曾经立志要过一种以责任为重的生活，我一直照此谨慎从事",
		"我有时阻止别人做某些事，并不是因为那种事有多大影响，而是在“道义”上我应该干预他",
		"我很容易生气，但很快就平静下来",
		"我已独立自主，不受家庭的约束",
		"我有很多心事",
		"我的亲属几乎全都同情我",
		"有时我十分烦躁，坐立不安",
		"我曾经失恋过",
		"我从来不为我的外貌而发愁",
		"我常梦到一些不可告人的事",
		"我相信我并不比别人更为神经过敏",
		"我几乎没有什么地方疼痛",
		"我的做事方法容易被人误解",
		"我的父母和家里人对我过于挑剔",
		"我的脖子（颈）上时常出现红斑",
		"我有理由妒忌我家里的某些人",
		"我有时无缘无故地，甚至在不顺利的时候也会觉得非常快乐",
		"我相信阴间有魔鬼和地狱",
		"有人想把世界上所能得到的东西都夺到手，我决不责怪他",
		"我曾经发呆（发愣）停止活动，不知道周围发生了什么事情",
		"谁也不关心谁的遭遇",
		"有些人所做的事，虽然我认为是错的，但我仍然能够友好地对待他们",
		"我喜欢和一些能互相开玩笑的人在一起",
		"在选举的时候，有时我会选出我不熟悉的人",
		"报纸上只有“漫画”最有趣",
		"凡是我所做的事，我都指望能够成功",
		"我相信有神",
		"做什么事情，我都感到难以开头",
		"在学校里，我是个笨学生",
		"如果我是个画家，我喜欢画花",
		"我虽然相貌不好看，也不因此而苦恼",
		"即使在冷天，我也很容易出汗",
		"我十分自信",
		"对任何人都不信任，是比较安全的",
		"每星期至少有一两次我十分兴奋",
		"人多的时候，我不知道说些什么话好",
		"在我心情不好的时候，总会有一些事使我高兴起来",
		"我能很容易使人怕我，有时我故意这样作来寻开心",
		"我离家外出的时候，从来不担心家里门窗是否关好锁好了",
		"我不责怪一个欺负了自找没趣的人",
		"我有时精力充沛",
		"我的皮肤上有一两处麻木了",
		"我的视力和往年一样好",
		"有人控制着我的思想",
		"我喜欢小孩子",
		"有时我非常欣赏骗子的机智，我甚至希望他能侥幸混过去",
		"我时常觉得有些陌生人用挑剔的眼光盯着我",
		"我每天喝特别多的水",
		"大多数人交朋友，是因为朋友对他们有用",
		"我很少注意我的耳鸣",
		"通常我爱家里的人，偶尔也恨他们",
		"假使我是一个新闻记者，我将很愿意报导体育新闻",
		"我确信别人正在议论我",
		"偶尔我听了下流的笑话也会发笑",
		"我独自一个人的时候，感到更快乐",
		"使我害怕的事比我的朋友们少得多",
		"恶心呕吐的毛病使我苦恼",
		"当一个罪犯可以通过能言善辩的律师开脱罪责时，我对法律感到厌恶",
		"我总是在很紧张的情况下工作的",
		"在我这一生中，至少有一两次我觉得有人用暗示指使我做了一些事",
		"我不愿意同人讲话，除非他先开口",
		"有人一直想要影响我的思想",
		"我从来没有犯过法",
		"我喜欢看《红楼梦》这一类的小说",
		"有些时候，我会无缘无故地觉得非常愉快",
		"我希望我不再受那种和性方面有关的念头所困扰",
		"假若有几个人闯了祸，他们最好先编一套假话，而且不改口",
		"我认为我比大多数人更重感情",
		"在我的一生中，从来没有喜欢过洋娃娃",
		"许多时候，生活对我来说是一件吃力的事",
		"我从来没有为了我的性方面的行为出过事",
		"对于某些事情我很敏感，以至使我不能提起",
		"在学校里，要我在班上发言，是非常困难的",
		"即使和人们在一起，我还是经常感到孤单",
		"应得的同情，我全得到了",
		"我拒绝玩那些我玩得不好的游戏",
		"有时我非常想离开家",
		"我交朋友差不多和别人一样地容易",
		"我的性生活是满意的",
		"我小的时候，有一段时间我干过小偷小摸的事",
		"我不喜欢有人在我身旁",
		"有人不将自己的贵重物品保管好因而引起别人偷窃，这种人和小偷一样应受责备",
		"偶尔我会想到一些坏得说不出口的事",
		"我深信生活对我是残酷的",
		"我想差不多每个人都会为了避免麻烦说点假话",
		"我比大多数人更敏感",
		"我的日常生活中，充满着使我感兴趣的事情",
		"大多数人都是内心不愿意挺身而出去帮助别人的",
		"我的梦有好些是关于性方面的事 ",
		"我很容易感到不知所措",
		"我为金钱和事业忧虑",
		"我曾经有过很特别，很奇怪的体验",
		"我从来没有爱上过任何人",
		"我家里有些人所做的事，使我吃惊",
		"有时我会哭一阵，笑一阵，连我自己也不能控制",
		"我的母亲或父亲时常要我服从他，即使我认为是不合理的",
		"我发现我很难把注意力集中到一件工作上",
		"我几乎从不做梦",
		"我从来没有瘫痪过或是感到肌肉非常软弱无力",
		"假如不是有人和我作对，我一定会有更大的成就",
		"即使我没有感冒，我有时也会发不出声音或声音改变",
		"似乎没有人能了解我",
		"有时我会闻到奇怪的气味",
		"我不能专心于一件事情上",
		"我很容易对人感到不耐烦",
		"我几乎整天都在为某件事或某个人而焦虑",
		"我所操心的事，远远超过了我所应该操心的",
		"大部分时间，我觉得我还是死了的好",
		"有时我会兴奋得难以入睡",
		"有时我的听觉太灵敏了，反而使我感到烦恼",
		"别人对我所说的话，我立刻就忘记了",
		"哪怕琐碎小事，我也再三考虑后才去做",
		"有时为避免和某些人相遇，我会绕道而行",
		"我常常觉得好象一切都不是真的",
		"我有一个习惯，喜欢点数一些不重要的东西，像路上的电线杆等等",
		"我没有真正想伤害我的仇人",
		"我提防那些对我过分亲近的人",
		"我有一些奇怪和特别的念头",
		"在我独处的时候，我听到奇怪的声音",
		"当我必须短期离家出门的时候，我会感到心神不定",
		"我怕一些东西或人，虽然我明知他们是不会伤害我的",
		"如果屋子里已经有人聚在一起谈话，这时要我一个人进去，我是一点也不怕的",
		"我害怕使用刀子或任何尖利的东西",
		"有时我喜欢折磨我所爱的人",
		"我似乎比别人更难于集中注意力",
		"有好几次我放弃正在做的事，因为我感到自己的能力太差了",
		"我脑子里出现一些坏的常常是可怕的字眼，却又无法摆脱它们",
		"有时一些无关紧要的念头缠着我，使我好多天都感到不安",
		"几乎每天都有使我害怕的事情发生",
		"我总是将事情看得严重些",
		"我比大多数人更敏感",
		"有时我喜欢受到我心爱的人的折磨",
		"有人用侮辱性的和下流的话议论我",
		"我呆在屋里总感到不安",
		"即使和人们在一起，我仍经常感到孤单",
		"我并不是特别害羞拘谨",
		"有时我的头脑似乎比平时迟钝",
		"在社交场合，我多半是一个人坐着，或者只跟另一个人坐在一起，而不到人群里去",
		"人们常使我失望",
		"我很喜欢参加舞会",
		"有时我常感到困难重重，无法克服",
		"我常想：“我要能再成为一个孩子就好了”",
		"如果给我机会，我一定能做些对世界大有益处的事",
		"我时常遇见一些所谓的专家，他们并不比我高明",
		"当我听说我所熟悉的人成功了，我就觉得自己失败了",
		"如果有机会，我一定能成为一个人民的好领袖",
		"下流的故事使我感到不好意思",
		"一般来说人们要求别人尊重他们自己比较多，而自己却很少尊重别人",
		"我总想把好的故事记住，讲给别人听",
		"我喜欢搞输赢不大的赌博",
		"为了可以和人们在一起，我喜欢社交活动",
		"我喜欢人多热闹的场合",
		"当我和一群活泼的朋友在一起的时候，我的烦恼就消失了",
		"当人们说我的班组人闲话时，我从来不参与",
		"只要我开始做一件事，就很难放下，哪怕是暂时的",
		"我的小便不困难，也不难控制",
		"我常发现别人妒忌我的好主意，因为他们没能先想到",
		"只要有可能，我就避开人群",
		"我不怕见生人",
		"记得我曾经为了不想做某件事而装过病",
		"在火车和公共汽车上，我常跟陌生人交谈",
		"当事情不顺利的时候，我就想立即放弃",
		"我愿意让人家知道我对于事物的态度",
		"有些时间，我感到劲头十足，以至一连几天都不需要睡觉",
		"在人群中，如果叫我带头发言，或对我所熟悉的事情发表意见，我并不感到不好意思",
		"我喜欢聚会和社交活动",
		"面对困难或危险的时候，我总退缩不前",
		"我原来想做的事，假若别人认为不值得做，我很容易放弃",
		"我不怕火",
		"我不怕水",
		"我常常是仔细考虑之后才做出决定",
		"生活在这个丰富多彩的时代里是多么美好",
		"当我想纠正别人的错误和帮助他们的时候，我的好意常被误解",
		"我吞咽没有困难",
		"我有时回避见人，因为我怕我会做出或讲出一些事后令我懊悔的事",
		"我通常很镇静，不容易激动",
		"我不轻意流露自己的感情，以至于人家得罪了我，他自己还不知道",
		"有时我因为承担的事情太多，以至精疲力竭",
		"我当然乐于以其人之道还治其人之身",
		"宗教不使我烦恼",
		"我生病或受伤的时候，不怕找医生",
		"我有罪，应受重罚",
		"我把失望的事看得太重，以至于总忘不了",
		"我很不喜欢匆匆忙忙地赶工作",
		"虽然我明知自己能把事做好，但是我也怕别人看着我",
		"在排队的时候如果有人插到我的前面去，我会感到恼火而指责他",
		"有时我觉得自己一无是处",
		"小时候我时常逃学",
		"我曾经有过很不寻常的宗教体验",
		"我家里有人很神经过敏",
		"我因为家里有的人所从事的职业而感到不好意思",
		"我很喜欢（或者喜欢过）钓鱼",
		"我几乎总是感到肚子饿",
		"我经常做梦",
		"我有时只好用不客气的态度去对付那些粗鲁或令人厌恶的人",
		"我倾向于对各种不同爱好发生兴趣，而不愿意长期坚持其中的某一种",
		"我喜欢阅读报纸的社论",
		"我喜欢听主题严肃的演说",
		"我易受异性的吸引",
		"我相当担心那些可能发生的不幸",
		"我有着坚定的政治见解",
		"我曾经有过想象的同伴",
		"我能够成为一个摩托车运动员",
		"我通常喜欢和妇女一起工作",
		"我觉得只有一种宗教是真的",
		"只要你不是真正的犯法，在法律的漏洞中取巧是可以的",
		"有些人讨厌极了，我会因为他们自食其果而暗中高兴",
		"要我等待我就紧张",
		"当我兴高彩烈的时候，见到别人忧郁消沉就使我大为扫兴",
		"我喜欢身材高的女人",
		"有些时期我因忧虑而失眠",
		"假若别人认为我对某些事的做法不妥当的话，我很容易放弃",
		"我不想去纠正那些发表愚昧无知见解的人",
		"我年轻（童年）的时候，喜欢热闹",
		"警察通常是诚实的",
		"当别人反对我的意见时，我会不惜一切去说服他",
		"在街上、车上、或在商店里，如果有人注视我，我会觉得不安",
		"我不喜欢看到妇女吸烟",
		"我很少有忧郁的毛病",
		"如果有人对我所熟悉的事情发表愚蠢和无知的意见，我总是设法纠正他",
		"我喜欢开别人的玩笑",
		"我小时候对是否参加团伙无所谓",
		"独自住在深山或老林的小木屋里，我也会觉得快乐",
		"许多人都说我是急性子",
		"如果一个人触犯了一条他认为不合理的法律，他是不应该受到惩罚的",
		"我认为一个人决不应该喝酒",
		"小时候和我关系密切的人（父亲．继父等）对我十分严厉",
		"我有几种坏习惯，已经根深蒂固，难于改正",
		"我只适量地喝一点酒（或者一点也不喝）",
		"我喜欢我能避免因为破口伤人而引起的烦恼",
		"我觉得不能把自己的一切都告诉别人",
		"我从前喜欢玩“跳房子”的游戏",
		"我从来没有见过幻像",
		"对于我的终身职业，我已经几次改变过主意",
		"除了医生的嘱咐，我从来不服用任何药物或安眠药",
		"我时常默记一些无关重要的号码（如汽车牌照等）",
		"我时常因为自己爱发脾气和抱怨而感到懊悔",
		"闪电是我害怕的东西中的一种",
		"有关性方面的事使我厌恶",
		"在学校中老师对我的品行评定总是很不好",
		"火对我有一种吸引力",
		"我喜欢让别人猜测我下一步的行动",
		"我的小便次数不比别人多",
		"万不得已的时候，我只吐露一些无损于自己的那部分真情",
		"我是神派来的特使",
		"假如我和几个朋友有着同样的过错，我宁可一人承担而不愿连累别人",
		"我还从来没有因为家里人惹了事而自己感到特别紧张",
		"人与之间的相互欺骗是我所知道的唯一奇迹",
		"我常常怕黑暗",
		"我害怕一个人单独呆在黑暗中",
		"我的计划看来总是困难重重，使我不得不一一放弃",
		"神创造奇迹",
		"有些缺点，我只好承认设法加以控制，但无法消除",
		"一个男人和一个女人相处的时候，他通常想到的是关于他的性方面的事",
		"我从来没有发现我尿中有血",
		"当我试图使别人不犯错误而做的事别人误解的时候，我往往感到十分难过",
		"每星期我祈祷几次",
		"我同情那些不能摆脱苦恼和忧愁的人",
		"我每星期念几次经",
		"对认为世界上只有一种宗教是真的那些人，我感到不耐烦",
		"我想起地震就害怕",
		"我喜欢那种需要集中注意力工作，而不喜欢省心的工作",
		"我怕自己被关在小房间里或紧闭的小地方",
		"对那些我想帮助他们改正或提高的人，我总是担率地交底",
		"我从来没有过将一件东西看成两件",
		"我喜欢探险小说",
		"坦率永远是一件好事",
		"我必须承认我有时会不合理地担心一些无关紧要的事情",
		"我很乐意百分之百地接受一个好意见",
		"我一向总是靠自己解决问题，而不是找人教我怎样做",
		"风暴使我惊慌",
		"我不经常对别人的行动表示强烈的赞成或反对",
		"我不想隐瞒我对一个人的坏印象或同情，以至他不知道我对他的看法",
		"我认为“不肯拉车的马应该受到鞭打”",
		"我是个神经高度紧张的人",
		"我经常遇到一些领导人，他们把功劳归于自己，把错误推给下级",
		"我相信我的嗅觉和别人一样好",
		"因为我太拘谨，所以有时我难以坚持自己的正确意见",
		"肮脏使我害怕或恶心",
		"我有一种不愿告诉别人的梦幻生活",
		"我不喜欢洗澡",
		"我认为为别人谋求幸福比为自己争取自由更为伟大",
		"我喜欢有男子气的女人",
		"我们家总是不愁吃不愁穿",
		"我家里有些人脾气急躁",
		"我无论什么事情都做不好",
		"我经常感到惭愧，因为我对某些事情想的和做的不一样",
		"我的性器官有些毛病",
		"我的原则是坚持强烈维护自己的意见",
		"我常常向别人请教",
		"我不害怕蜘蛛",
		"我从来不脸红",
		"我不怕从门把上传染上疾病",
		"有些动物使我神经紧张",
		"我的前途似乎没有希望",
		"我家里人和近亲们相处得很好",
		"我并不容易比别人脸红",
		"我喜欢穿高档的衣服",
		"我常常担心自己会脸红",
		"即使我以为自己对某件事已经打定了主意，别人也很容易使我变卦",
		"我和别人一样能够经受同量的痛苦",
		"我并不因为常常打嗝而觉得很烦恼",
		"有好几次都是我一个人坚持到底，最后才放弃了所做的事",
		"我几乎整天感到口干",
		"只要有人催我，我就生气",
		"我想去深山野林中打老虎",
		"我想我会喜欢裁缝的工作",
		"我不怕老鼠",
		"我的面部从来没有麻痹过",
		"我的皮肤似乎对触觉特别敏感",
		"我从来没有过像柏油一样的黑粪便",
		"每星期我总有几次觉得好像有可怕的事情要发生",
		"我大部分时间都感到疲倦",
		"有时我一再做同样的梦",
		"我喜欢阅读有关历史的书籍",
		"未来是变化无常的，一个人很难做出认真的安排",
		"如果可以避免的话，我决不去看色情的表演",
		"许多时候，即使一切顺利，我对任何事情都觉得无所谓",
		"我喜欢修理门锁",
		"有时我可以肯定别人知道我在想什么",
		"我喜欢阅读有关科学的书籍",
		"我害怕单独呆在空旷的地方",
		"假如我是个画家，我喜欢画小孩子",
		"有时我觉得我就要垮了",
		"我很注意我的衣着式样",
		"我喜欢当一个私人秘书",
		"许多人都因为有过不良的性行为而感到惭愧",
		"我经常在半夜里受到惊吓",
		"我经常因为记不清把东西放在哪里而感到苦恼",
		"我很喜欢骑马",
		"小时候，我最依恋和钦佩的是一个女人（母亲、姐姐、姑、婶、姨、等等）",
		"我喜欢探险小说胜过爱情小说",
		"我不轻易生气",
		"当我站在高处的时候，我就很想往下跳",
		"我喜欢电影里的爱情镜头",
		"你的性别",
		
		
	};
	
}
