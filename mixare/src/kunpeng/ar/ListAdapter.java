
package kunpeng.ar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

	public class ListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private List<Map<String, Object>> mData;
		private boolean isSmallList;
		public ListAdapter(Context context,List<Map<String, Object>> list,boolean isSmallList) {
			this.mInflater = LayoutInflater.from(context);
			this.isSmallList = isSmallList;
			setData(list);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mData.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}
		public void setData(List<Map<String, Object>> list){
			mData = new ArrayList<Map<String, Object>>(list);
		}
		public List<Map<String, Object>> getData() {
			return mData;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			if (convertView == null) {

				holder = new ViewHolder();

				if(isSmallList){
					convertView = mInflater.inflate(R.layout.vlistsmall, null);
				}else{
					convertView = mInflater.inflate(R.layout.vlist, null);
				}
				holder.img = (ImageView) convertView.findViewById(R.id.img);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.info = (TextView) convertView.findViewById(R.id.info);
				convertView.setTag(holder);

			} else {

				holder = (ViewHolder) convertView.getTag();
			}

			holder.img.setBackgroundResource((Integer) mData.get(position).get(
					"img"));
			holder.title.setText((String) mData.get(position).get("title"));
			holder.info.setText((String) mData.get(position).get("info"));



			return convertView;
		}
		public final class ViewHolder{
			public ImageView img;
			public TextView title;
			public TextView info;
		}
	}