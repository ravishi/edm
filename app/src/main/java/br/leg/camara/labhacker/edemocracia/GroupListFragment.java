package br.leg.camara.labhacker.edemocracia;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.liferay.mobile.android.v62.group.GroupService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import javax.annotation.Nullable;

import br.leg.camara.labhacker.edemocracia.content.Group;
import br.leg.camara.labhacker.edemocracia.util.EDMSession;
import br.leg.camara.labhacker.edemocracia.util.JSONReader;


public class GroupListFragment extends ContentListFragment<Group> {

    private OnGroupSelectedListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnGroupSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnGroupSelectedListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (listener != null) {
            listener.onGroupSelected((Group) getListAdapter().getItem(position));
        }
    }

    @Override
    protected List<Group> fetchItems() throws Exception {
        EDMSession session = EDMSession.get(getActivity().getApplicationContext());

        assert session != null;

        GroupService groupService = new GroupService(session);

        JSONArray jsonGroups = groupService.search(
                session.getCompanyId(), "%", "%", new JSONArray(), -1, -1);

        List<Group> groups = JSONReader.fromJSON(jsonGroups, Group.JSON_READER);

        return Lists.newArrayList(Collections2.filter(groups, new Predicate<Group>() {
            @Override
            public boolean apply(@Nullable Group group) {
                return group != null && group.isActive() && group.getType() != 1;
            }
        }));
    }

    public interface OnGroupSelectedListener {
        public void onGroupSelected(Group group);
    }
}
