package emerald.apps.fairychess.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import emerald.apps.fairychess.R


class GameListAdapter(val context: Context,
                      private val dataSource: List<MainActivityListener.Game>) : BaseAdapter() {


    private val layoutInflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {return dataSource.size}
    override fun getItem(position: Int): Any {return dataSource[position]}
    override fun getItemId(position: Int): Long { return position.toLong()}

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var rowView : View
        if(convertView == null){
            rowView = layoutInflater.inflate(R.layout.game_rowview,parent,false)
        } else {
            rowView = convertView
        }

        val tvGamename = rowView.findViewById<TextView>(R.id.tv_gameName)
        tvGamename.text = dataSource[position].gameName
        val tvTimemode = rowView.findViewById<TextView>(R.id.tv_timeMode)
        tvTimemode.text = dataSource[position].timeMode


        return rowView
    }

}