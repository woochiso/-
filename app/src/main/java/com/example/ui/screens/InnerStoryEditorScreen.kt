package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.*
import com.example.ui.theme.AppActionButton
import java.time.Year

@Composable fun InnerStoryEditorScreen(initial:StoryDto?=null,onCancel:()->Unit,onCreate:(StoryWriteRequest)->Unit,onUpdate:(StoryUpdateRequest)->Unit,modifier:Modifier=Modifier){
 var title by remember(initial){mutableStateOf(initial?.title.orEmpty())};var content by remember(initial){mutableStateOf(initial?.content.orEmpty())};var period by remember(initial){mutableStateOf(initial?.period.orEmpty())};var ageGroup by remember(initial){mutableStateOf(ageGroupLabel(initial?.age))};var year by remember(initial){mutableStateOf(if(initial==null)"1975" else initial.year?.toString().orEmpty())};var category by remember(initial){mutableStateOf(categoryLabel(initial?.eventCategory))};var person by remember(initial){mutableStateOf(personLabel(initial?.relatedPerson))};var importance by remember(initial){mutableIntStateOf(initial?.importance?:1)};var impact by remember(initial){mutableIntStateOf(initial?.currentImpact?:1)};var status by remember(initial){mutableStateOf(statusLabel(initial?.currentStatus))}
 val valid=title.isNotBlank()&&title.length<=150&&content.isNotBlank()&&content.length<=5000&&period.isNotBlank()&&period.length<=100&&(year.isBlank()||year.toIntOrNull() in 1900..Year.now().value)
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){IconButton(onClick=onCancel){Icon(Icons.AutoMirrored.Filled.ArrowBack,"목록으로 돌아가기")};Column{Text(if(initial==null)"나의 사연 작성" else "나의 사연 수정",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("마음속에 남아 있는 이야기를 천천히 기록해보세요.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
  item{OutlinedTextField(title,{title=it},label={Text("사연 제목")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),singleLine=true,modifier=Modifier.fillMaxWidth())}
  item{OutlinedTextField(period,{period=it},label={Text("사건이 일어난 시기")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),singleLine=true,modifier=Modifier.fillMaxWidth())}
  item{Choice("사건 당시 나이",ageGroup,ageGroupLabels){ageGroup=it}}
  item{OutlinedTextField(year,{year=it.filter(Char::isDigit).take(4)},label={Text("사건 연도")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())}
  item{Choice("사건 영역",category,categoryLabels){category=it}}
  item{Choice("관련된 사람",person,personLabels){person=it}}
  item{OutlinedTextField(content,{content=it},label={Text("사연 내용")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),minLines=6,supportingText={Text("${content.length} / 5000자",modifier=Modifier.fillMaxWidth())},modifier=Modifier.fillMaxWidth())}
  item{Rating("내 인생에 얼마나 큰 사건이었나요?",importance){importance=it}}
  item{Rating("현재도 얼마나 영향을 준다고 느끼나요?",impact){impact=it}}
  item{Choice("현재 상태",status,statusLabels){status=it}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onCancel,Modifier.weight(1f)){Text("취소")};Button(onClick={val w=StoryWriteRequest(title.trim(),content.trim(),period.trim(),ageGroupValue(ageGroup),year.toIntOrNull(),categoryValue(category),personValue(person),importance,impact,statusValue(status));if(initial==null)onCreate(w)else onUpdate(StoryUpdateRequest(initial.storyId,w.title,w.content,w.period,w.age,w.year,w.eventCategory,w.relatedPerson,w.importance,w.currentImpact,w.currentStatus))},enabled=valid,colors=ButtonDefaults.buttonColors(containerColor=AppActionButton,contentColor=Color.White),modifier=Modifier.weight(1f)){Text("사연 저장")}}}
 }
}
@Composable private fun Rating(q:String,v:Int,set:(Int)->Unit){Column{Text(q,fontWeight=FontWeight.SemiBold);Row{(1..5).forEach{i->IconButton({set(i)}){Icon(if(i<=v)Icons.Filled.Star else Icons.Outlined.StarOutline,"${i}점",tint=AppActionButton)}}}}}
@Composable private fun Choice(label:String,value:String,options:List<String>,set:(String)->Unit){var open by remember{mutableStateOf(false)};Box{OutlinedTextField(value,{},readOnly=true,label={Text(label)},trailingIcon={Icon(Icons.Default.ArrowDropDown,null)},modifier=Modifier.fillMaxWidth());Box(Modifier.fillMaxWidth().height(56.dp).clickable{open=true});DropdownMenu(open,{open=false}){options.forEach{DropdownMenuItem({Text(it)},{set(it);open=false})}}}}
private val categoryLabels=listOf("나 자신","가족","친구","연인","학교","직장","경제","건강","사회","기타");private val categoryValues=listOf("SELF","FAMILY","FRIEND","LOVER","SCHOOL","WORK","MONEY","HEALTH","SOCIETY","OTHER")
private val personLabels=listOf("부모","형제자매","배우자","연인","친구","교사","상사","동료","학교 친구","낯선 사람","기타");private val personValues=listOf("PARENT","SIBLING","SPOUSE","LOVER","FRIEND","TEACHER","BOSS","COLLEAGUE","CLASSMATE","STRANGER","OTHER")
private val statusLabels=listOf("아직 많이 영향을 받고 있음","조금씩 나아지고 있음","지금은 많이 괜찮아짐");private val statusValues=listOf("NOT_RESOLVED","IMPROVING","RESOLVED")
private val ageGroupLabels=listOf("선택","10세 미만","10대","20대","30대","40대","50대","60대","70대","80대 이상");private val ageGroupValues=listOf(null,0,10,20,30,40,50,60,70,80)
private fun ageGroupValue(label:String)=ageGroupValues.getOrNull(ageGroupLabels.indexOf(label));private fun ageGroupLabel(age:Int?):String=when{age==null->"선택";age<10->"10세 미만";age>=80->"80대 이상";else->"${(age/10)*10}대"}
private fun categoryValue(x:String)=categoryValues.getOrElse(categoryLabels.indexOf(x)){"OTHER"};private fun categoryLabel(x:String?)=categoryLabels.getOrElse(categoryValues.indexOf(x)){categoryLabels.first()};private fun personValue(x:String)=personValues.getOrElse(personLabels.indexOf(x)){"OTHER"};private fun personLabel(x:String?)=personLabels.getOrElse(personValues.indexOf(x)){personLabels.first()};private fun statusValue(x:String)=statusValues.getOrElse(statusLabels.indexOf(x)){"NOT_RESOLVED"};private fun statusLabel(x:String?)=statusLabels.getOrElse(statusValues.indexOf(x)){statusLabels.first()}
