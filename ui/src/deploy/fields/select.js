import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class Select extends React.Component {

    handleValidationFormBasicText() {
        return null;
    }

    handleChange = (e) => {
        this.props.onChange(e.target.value);
    };

    render() {
        let multiple = false;
        return (
            <FormGroup controlId="formControlsSelect">
                <div className="input-group mb-3">
                    <FormControl.Feedback/>
                    <FormControl
                        componentClass="select"
                        placeholder={this.props.value}
                        multiple={multiple}
                        onChange={this.handleChange}
                        value={this.props.value}
                    >
                        {this.props.param.values.map(function (value, i) {
                            return (
                                <option key={i} value={value.value}>
                                    {value.label}
                                    {(value.label !== undefined && value.description !== undefined) ? " - " : ""}
                                    {value.description}
                                </option>
                            );
                        }, this)}
                    </FormControl>
                    <div className="input-group-append">
                        <RemoveButton
                            canRemove={this.props.canRemove}
                            onRemove={this.props.onRemove}
                            nextToInput={true}
                        />
                    </div>
                </div>
            </FormGroup>
        );
    }
}