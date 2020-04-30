<template>
    <div>
      <v-layout class="px-3 py-2">
        <v-flex xs2>
          <v-btn color="info">新增品牌</v-btn>
        </v-flex>
        <v-spacer/>
        <v-flex xs4>
          <v-text-field label="搜索" hide-details append-icon="search" v-model="search"/>
        </v-flex>
      </v-layout>
      <v-data-table
        :headers="headers"
        :items="brands"
        :pagination.sync="pagination"
        :total-items="totalBrands"
        :loading="loading"
        class="elevation-1"
      >
        <template slot="items" slot-scope="props">
          <td class="text-xs-center">{{ props.item.id }}</td>
          <td class="text-xs-center">{{ props.item.name }}</td>
          <td class="text-xs-center"><img :src="props.item.image" /></td>
          <td class="text-xs-center">{{ props.item.letter }}</td>
          <td class="text-xs-center">
            <v-btn text icon color="info" small>
              <v-icon>edit</v-icon>
            </v-btn>
            <v-btn text icon color="error" small>
              <v-icon>delete</v-icon>
            </v-btn>
          </td>
        </template>
      </v-data-table>
    </div>
</template>

<script>
    export default {
        name: "MyBrand",
        data(){
          return{
            headers:[
              {text: '品牌ID', align: 'center', sortable: true, value: 'id'},
              {text: '品牌名称', align: 'center', sortable: false, value: 'name'},
              {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
              {text: '品牌名称首字母', align: 'center', sortable: true, value: 'letter'},
              {text: '操作', align: 'center', sortable: false}
            ],
            brands:[],      //当前页品牌数据
            pagination:{},  //分页信息
            totalBrands:0,  //数据总条数
            loading:false,  //是否正在加载
            search:"", //搜索条件
          }
        },
      created(){
        this.totalBrands = 15;
        this.brands = [
          {id: 2032, name: "OPPO", image: "http://img10.360buyimg.com/popshop/jfs/t2119/133/2264148064/4303/b8ab3755/56b2f385N8e4eb051.jpg", letter: "O"},
          {id: 2033, name: "飞利浦（PHILIPS）", image: "http://img12.360buyimg.com/popshop/jfs/t18361/122/1318410299/1870/36fe70c9/5ac43a4dNa44a0ce0.jpg", letter: "F"},
          {id: 2034, name: "华为（HUAWEI）", image: "http://img10.360buyimg.com/popshop/jfs/t5662/36/8888655583/7806/1c629c01/598033b4Nd6055897.jpg", letter: "H"},
          {id: 2036, name: "酷派（Coolpad）", image: "http://img10.360buyimg.com/popshop/jfs/t2521/347/883897149/3732/91c917ec/5670cf96Ncffa2ae6.jpg", letter: "K"},
          {id: 2037, name: "魅族（MEIZU）", image: "http://img13.360buyimg.com/popshop/jfs/t3511/131/31887105/4943/48f83fa9/57fdf4b8N6e95624d.jpg", letter: "M"}
        ];


        //从后台查询数据
        this.loadBrands();
      },
      watch:{
        search(){ //监视搜索字段
          this.pagination.page = 1; //每次查询时将当前的页码，改为以1
          this.loadBrands();
        },
        pagination:{  // 监视pagination属性的变化
          deep: true, // deep为true，会监视pagination的属性及属性中的对象属性变化
          handler(){
            //变化后的回调函数，这里再次调用loadBrands即可
            this.loadBrands();
          }
        }
      },
      methods:{
          loadBrands(){   //从后台加载数据
            this.loading = true;
            this.$http.get("/item/brand/page", {            // 发起请求
              params:{
                page: this.pagination.page, //当前页
                rows: this.pagination.rowsPerPage, //当前页的大小
                sortBy: this.pagination.sortBy, //排序字段
                desc: this.pagination.descending, //是否降序
                search: this.search //搜索条件
              }
            }).then(resp => {
              this.brands = resp.data.items;
              this.totalBrands = resp.data.total;
              this.loading = false;
            })
          }
      }
    }
</script>

<style scoped>

</style>
